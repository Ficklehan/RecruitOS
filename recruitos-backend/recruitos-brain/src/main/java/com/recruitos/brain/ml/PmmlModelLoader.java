package com.recruitos.brain.ml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import java.io.*;
import java.util.*;

/**
 * PMML 模型加载器 — 加载 LightGBM 导出的 PMML 模型文件并执行推理。
 * 
 * <p>使用 DOM 解析 PMML XML，提取 MiningModel/Segmentation 结构中的
 * TreeModel 节点树，在纯 Java 中执行预测。不依赖 JPMML 运行时，避免重量级依赖。
 * 
 * <p>PMML 由 sklearn2pmml 从 LightGBM LGBMClassifier 导出。
 */
public class PmmlModelLoader {
    private static final Logger log = LoggerFactory.getLogger(PmmlModelLoader.class);

    private final List<TreeEnsemble> trees = new ArrayList<>();
    private int numClasses;
    private List<String> featureNames;
    private final Map<String, Double> featureMeans = new LinkedHashMap<>();
    private final Map<String, Double> featureStds = new LinkedHashMap<>();
    private boolean loaded = false;

    /**
     * 从 PMML 文件路径加载模型。
     * @param pmmlPath PMML 文件路径（classpath或绝对路径）
     */
    public PmmlModelLoader(String pmmlPath) {
        try {
            Document doc = parsePmml(pmmlPath);
            Element root = doc.getDocumentElement();
            
            // 解析特征名称
            this.featureNames = parseFeatureNames(root);
            
            // 解析标准化参数
            parseTransformations(root);
            
            // 解析 MiningModel → Segmentation → TreeModels
            NodeList miningModels = root.getElementsByTagName("MiningModel");
            if (miningModels.getLength() > 0) {
                Element miningModel = (Element) miningModels.item(0);
                NodeList segments = miningModel.getElementsByTagName("Segment");
                for (int i = 0; i < segments.getLength(); i++) {
                    Element seg = (Element) segments.item(i);
                    NodeList treeModels = seg.getElementsByTagName("TreeModel");
                    for (int j = 0; j < treeModels.getLength(); j++) {
                        trees.add(parseTreeModel((Element) treeModels.item(j)));
                    }
                }
            }
            
            // 如果没有 MiningModel，直接查找 TreeModel
            if (trees.isEmpty()) {
                NodeList treeModels = root.getElementsByTagName("TreeModel");
                for (int i = 0; i < treeModels.getLength(); i++) {
                    trees.add(parseTreeModel((Element) treeModels.item(i)));
                }
            }
            
            this.numClasses = determineNumClasses(root);
            this.loaded = !trees.isEmpty();
            log.info("PMML loaded: {} trees, {} features, {} classes. Path: {}",
                trees.size(), featureNames.size(), numClasses, pmmlPath);
        } catch (Exception e) {
            log.error("Failed to load PMML model: {}", pmmlPath, e);
            this.numClasses = 2;
            this.featureNames = FeatureExtractor.FEATURE_NAMES.length > 0 
                ? Arrays.asList(FeatureExtractor.FEATURE_NAMES) : Collections.emptyList();
            this.loaded = false;
        }
    }

    public boolean isLoaded() { return loaded; }
    public int getNumTrees() { return trees.size(); }
    public List<String> getFeatureNames() { return featureNames; }

    /**
     * 对特征向量进行预测。
     * @param features 特征值数组（顺序需与 getFeatureNames() 一致）
     * @return 正类概率 [0, 1]
     */
    public double predictProba(double[] features) {
        if (!loaded || trees.isEmpty()) return 0.5;
        
        // 标准化
        double[] normalized = normalize(features);
        
        // 每棵树投票
        double sum = 0;
        for (TreeEnsemble tree : trees) {
            sum += tree.predict(normalized);
        }
        // sigmoid 转换
        return 1.0 / (1.0 + Math.exp(-sum));
    }

    private double[] normalize(double[] features) {
        if (featureMeans.isEmpty()) return features;
        double[] out = new double[features.length];
        for (int i = 0; i < features.length && i < featureNames.size(); i++) {
            String name = featureNames.get(i);
            Double mean = featureMeans.get(name);
            Double std = featureStds.get(name);
            if (mean != null && std != null && std > 0) {
                out[i] = (features[i] - mean) / std;
            } else {
                out[i] = features[i];
            }
        }
        return out;
    }

    // === PMML 解析 ===

    private Document parsePmml(String path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputStream is;
        File file = new File(path);
        if (file.exists()) {
            is = new FileInputStream(file);
        } else {
            is = getClass().getClassLoader().getResourceAsStream(path);
            if (is == null) throw new FileNotFoundException("PMML file not found: " + path);
        }
        try {
            return builder.parse(is);
        } finally {
            if (is != null) is.close();
        }
    }

    private List<String> parseFeatureNames(Element root) {
        List<String> names = new ArrayList<>();
        NodeList fields = root.getElementsByTagName("DataField");
        for (int i = 0; i < fields.getLength(); i++) {
            Element field = (Element) fields.item(i);
            String name = field.getAttribute("name");
            String optype = field.getAttribute("optype");
            if ("continuous".equals(optype)) names.add(name);
        }
        return names;
    }

    private void parseTransformations(Element root) {
        NodeList derived = root.getElementsByTagName("DerivedField");
        for (int i = 0; i < derived.getLength(); i++) {
            Element df = (Element) derived.item(i);
            String name = df.getAttribute("name");
            NodeList norms = df.getElementsByTagName("NormContinuous");
            if (norms.getLength() > 0) {
                Element norm = (Element) norms.item(0);
                String field = norm.getAttribute("field");
                NodeList linNorms = norm.getElementsByTagName("LinearNorm");
                if (linNorms.getLength() >= 2) {
                    Element ln1 = (Element) linNorms.item(0);
                    Element ln2 = (Element) linNorms.item(1);
                    double orig1 = Double.parseDouble(ln1.getAttribute("orig"));
                    double norm1 = Double.parseDouble(ln1.getAttribute("norm"));
                    double orig2 = Double.parseDouble(ln2.getAttribute("orig"));
                    double norm2 = Double.parseDouble(ln2.getAttribute("norm"));
                    double std = (orig2 - orig1) / (norm2 - norm1);
                    double mean = orig1 - norm1 * std;
                    featureMeans.put(field, mean);
                    featureStds.put(field, std);
                }
            }
        }
    }

    private TreeEnsemble parseTreeModel(Element treeModel) {
        NodeList nodes = treeModel.getElementsByTagName("Node");
        if (nodes.getLength() == 0) return new TreeEnsemble(Collections.emptyList());
        
        List<TreeNode> treeNodes = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            String id = node.getAttribute("id");
            String scoreStr = node.getAttribute("score");
            double score = scoreStr.isEmpty() ? 0 : Double.parseDouble(scoreStr);
            
            NodeList children = node.getElementsByTagName("Node");
            // 叶节点：没有 SimplePredicate 子节点的 Node
            NodeList predicates = node.getElementsByTagName("SimplePredicate");
            if (predicates.getLength() == 0) {
                treeNodes.add(new TreeNode(id, score, -1, 0, null, null));
            } else {
                Element pred = (Element) predicates.item(0);
                String field = pred.getAttribute("field");
                String operator = pred.getAttribute("operator");
                double value = Double.parseDouble(pred.getAttribute("value"));
                // 找到两个子节点ID
                int childrenInNode = 0;
                String leftId = null, rightId = null;
                for (int j = 0; j < children.getLength(); j++) {
                    Element child = (Element) children.item(j);
                    String childId = child.getAttribute("id");
                    if (childrenInNode == 0) leftId = childId;
                    else if (childrenInNode == 1) rightId = childId;
                    childrenInNode++;
                }
                int fieldIdx = featureNames.indexOf(field);
                treeNodes.add(new TreeNode(id, score, fieldIdx, value, leftId, rightId));
            }
        }
        return new TreeEnsemble(treeNodes);
    }

    private int determineNumClasses(Element root) {
        NodeList outputs = root.getElementsByTagName("OutputField");
        for (int i = 0; i < outputs.getLength(); i++) {
            Element out = (Element) outputs.item(i);
            if ("predictedValue".equals(out.getAttribute("name"))) {
                return 2; // 二分类
            }
        }
        return 2;
    }

    // === 内部类 ===

    static class TreeNode {
        final String id;
        final double score;
        final int featureIndex;   // -1 = leaf
        final double threshold;
        final String leftId;
        final String rightId;

        TreeNode(String id, double score, int featureIndex, double threshold, String leftId, String rightId) {
            this.id = id; this.score = score;
            this.featureIndex = featureIndex; this.threshold = threshold;
            this.leftId = leftId; this.rightId = rightId;
        }
        boolean isLeaf() { return featureIndex < 0; }
    }

    static class TreeEnsemble {
        final List<TreeNode> nodes;
        TreeNode root;

        TreeEnsemble(List<TreeNode> nodes) {
            this.nodes = nodes;
            // 第一个节点是根节点（PMML 按深度优先排列）
            if (!nodes.isEmpty()) this.root = nodes.get(0);
        }

        double predict(double[] features) {
            if (root == null) return 0;
            TreeNode node = root;
            while (!node.isLeaf()) {
                if (node.featureIndex >= 0 && node.featureIndex < features.length
                    && features[node.featureIndex] < node.threshold) {
                    node = findNode(node.leftId);
                } else {
                    node = findNode(node.rightId);
                }
                if (node == null) return 0;
            }
            return node.score;
        }

        private TreeNode findNode(String id) {
            if (id == null) return null;
            for (TreeNode n : nodes) {
                if (id.equals(n.id)) return n;
            }
            return null;
        }
    }
}
