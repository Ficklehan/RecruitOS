package com.recruitos.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.agent.dto.RecruitmentChannelCreateDTO;
import com.recruitos.agent.dto.RecruitmentChannelQueryDTO;
import com.recruitos.agent.dto.RecruitmentChannelVO;
import com.recruitos.agent.entity.AgentAccount;
import com.recruitos.agent.entity.RecruitmentChannel;
import com.recruitos.agent.mapper.AgentAccountMapper;
import com.recruitos.agent.mapper.RecruitmentChannelMapper;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;

@Service
public class RecruitmentChannelService {

    private static final List<String> SUPPORTED_PLATFORMS =
            Collections.unmodifiableList(Arrays.asList("BOSS", "LIEPIN"));

    @Resource
    private RecruitmentChannelMapper channelMapper;
    @Resource
    private AgentAccountMapper accountMapper;

    public void ensureDefaultChannels(Long tenantId) {
        LambdaQueryWrapper<RecruitmentChannel> w = new LambdaQueryWrapper<>();
        w.eq(RecruitmentChannel::getTenantId, tenantId);
        if (channelMapper.selectCount(w) > 0) {
            return;
        }
        for (Object[] row : defaultChannelRows()) {
            RecruitmentChannel ch = new RecruitmentChannel();
            ch.setTenantId(tenantId);
            ch.setChannelCode((String) row[0]);
            ch.setChannelName((String) row[1]);
            ch.setChannelType((String) row[2]);
            ch.setPlatformCode((String) row[3]);
            ch.setDescription((String) row[4]);
            ch.setStatus((String) row[5]);
            ch.setSortOrder((Integer) row[6]);
            ch.setSupportsAgent((Integer) row[7]);
            ch.setIsSystem(1);
            channelMapper.insert(ch);
        }
    }

    public RecruitmentChannelVO createChannel(RecruitmentChannelCreateDTO dto) {
        throw new BizException("当前暂不支持新增渠道，仅开放 Boss直聘 与 猎聘");
    }

    public RecruitmentChannelVO updateChannel(Long id, RecruitmentChannelCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        RecruitmentChannel ch = requireChannel(id, tenantId);

        if (StringUtils.hasText(dto.getChannelName())) {
            ch.setChannelName(dto.getChannelName().trim());
        }
        if (StringUtils.hasText(dto.getDescription())) {
            ch.setDescription(dto.getDescription());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            ch.setStatus(dto.getStatus());
        }
        if (dto.getSortOrder() != null) {
            ch.setSortOrder(dto.getSortOrder());
        }
        if (!SUPPORTED_PLATFORMS.contains(ch.getChannelCode())) {
            throw new BizException("当前仅支持 Boss直聘 与 猎聘 渠道");
        }
        channelMapper.updateById(ch);
        return toVO(ch, countAccounts(tenantId, ch.getId()));
    }

    public RecruitmentChannelVO getChannelDetail(Long id) {
        Long tenantId = TenantContext.getTenantId();
        RecruitmentChannel ch = requireChannel(id, tenantId);
        return toVO(ch, countAccounts(tenantId, ch.getId()));
    }

    public PageResult<RecruitmentChannelVO> listChannels(RecruitmentChannelQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();
        ensureDefaultChannels(tenantId);

        Page<RecruitmentChannel> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<RecruitmentChannel> w = new LambdaQueryWrapper<>();
        w.eq(RecruitmentChannel::getTenantId, tenantId);
        applySupportedPlatformScope(w);

        if (StringUtils.hasText(query.getChannelType())) {
            w.eq(RecruitmentChannel::getChannelType, query.getChannelType());
        }
        if (StringUtils.hasText(query.getStatus())) {
            w.eq(RecruitmentChannel::getStatus, query.getStatus());
        }
        if (Boolean.TRUE.equals(query.getSupportsAgentOnly())) {
            w.eq(RecruitmentChannel::getSupportsAgent, 1);
        }
        if (StringUtils.hasText(query.getKeyword())) {
            w.and(q -> q.like(RecruitmentChannel::getChannelName, query.getKeyword())
                    .or().like(RecruitmentChannel::getChannelCode, query.getKeyword()));
        }
        w.orderByAsc(RecruitmentChannel::getSortOrder).orderByAsc(RecruitmentChannel::getId);

        Page<RecruitmentChannel> result = channelMapper.selectPage(page, w);
        List<RecruitmentChannelVO> list = new ArrayList<>();
        for (RecruitmentChannel ch : result.getRecords()) {
            list.add(toVO(ch, countAccounts(tenantId, ch.getId())));
        }
        return new PageResult<>(result.getTotal(), list, query.getPageNum(), query.getPageSize());
    }

    public List<RecruitmentChannelVO> listActiveAgentChannels() {
        Long tenantId = TenantContext.getTenantId();
        ensureDefaultChannels(tenantId);

        LambdaQueryWrapper<RecruitmentChannel> w = new LambdaQueryWrapper<>();
        w.eq(RecruitmentChannel::getTenantId, tenantId)
                .eq(RecruitmentChannel::getStatus, "ACTIVE")
                .eq(RecruitmentChannel::getSupportsAgent, 1)
                .in(RecruitmentChannel::getPlatformCode, SUPPORTED_PLATFORMS)
                .orderByAsc(RecruitmentChannel::getSortOrder);
        List<RecruitmentChannelVO> list = new ArrayList<>();
        for (RecruitmentChannel ch : channelMapper.selectList(w)) {
            list.add(toVO(ch, countAccounts(tenantId, ch.getId())));
        }
        return list;
    }

    public void deleteChannel(Long id) {
        Long tenantId = TenantContext.getTenantId();
        RecruitmentChannel ch = requireChannel(id, tenantId);
        if (ch.getIsSystem() != null && ch.getIsSystem() == 1) {
            throw new BizException("系统预置渠道不可删除，可改为停用");
        }
        AccountCount count = countAccounts(tenantId, id);
        if (count.total > 0) {
            throw new BizException("渠道下仍有 " + count.total + " 个账号，请先迁移或删除账号");
        }
        channelMapper.deleteById(id);
    }

    public Map<String, Object> getStats() {
        Long tenantId = TenantContext.getTenantId();
        ensureDefaultChannels(tenantId);

        Map<String, Object> stats = new LinkedHashMap<>();
        LambdaQueryWrapper<RecruitmentChannel> totalW = new LambdaQueryWrapper<>();
        totalW.eq(RecruitmentChannel::getTenantId, tenantId);
        applySupportedPlatformScope(totalW);
        stats.put("totalChannels", channelMapper.selectCount(totalW));

        LambdaQueryWrapper<RecruitmentChannel> activeW = new LambdaQueryWrapper<>();
        activeW.eq(RecruitmentChannel::getTenantId, tenantId).eq(RecruitmentChannel::getStatus, "ACTIVE");
        applySupportedPlatformScope(activeW);
        stats.put("activeChannels", channelMapper.selectCount(activeW));

        LambdaQueryWrapper<RecruitmentChannel> agentW = new LambdaQueryWrapper<>();
        agentW.eq(RecruitmentChannel::getTenantId, tenantId).eq(RecruitmentChannel::getSupportsAgent, 1);
        applySupportedPlatformScope(agentW);
        stats.put("agentChannels", channelMapper.selectCount(agentW));

        LambdaQueryWrapper<AgentAccount> aw = new LambdaQueryWrapper<>();
        aw.eq(AgentAccount::getTenantId, tenantId);
        stats.put("totalAccounts", accountMapper.selectCount(aw));

        aw.eq(AgentAccount::getStatus, "ACTIVE");
        stats.put("activeAccounts", accountMapper.selectCount(aw));
        return stats;
    }

    public RecruitmentChannel requireChannelForAccount(Long channelId, Long tenantId) {
        RecruitmentChannel ch = requireChannel(channelId, tenantId);
        if (!"PLATFORM".equals(ch.getChannelType())) {
            throw new BizException("仅平台类渠道可绑定 Agent 账号");
        }
        if (!"ACTIVE".equals(ch.getStatus())) {
            throw new BizException("渠道已停用，无法添加账号");
        }
        return ch;
    }

    private RecruitmentChannel requireChannel(Long id, Long tenantId) {
        RecruitmentChannel ch = channelMapper.selectById(id);
        if (ch == null || !ch.getTenantId().equals(tenantId)) {
            throw new BizException("渠道不存在");
        }
        return ch;
    }

    private void validateCreate(RecruitmentChannelCreateDTO dto) {
        if (!StringUtils.hasText(dto.getChannelCode())) {
            throw new BizException("渠道编码不能为空");
        }
        if (!StringUtils.hasText(dto.getChannelName())) {
            throw new BizException("渠道名称不能为空");
        }
        if (!StringUtils.hasText(dto.getChannelType())) {
            throw new BizException("渠道类型不能为空");
        }
        Set<String> types = new HashSet<>(Arrays.asList("PLATFORM", "REFERRAL", "HEADHUNTER", "DIRECT", "CAREERS", "CUSTOM"));
        if (!types.contains(dto.getChannelType())) {
            throw new BizException("不支持的渠道类型");
        }
        if ("PLATFORM".equals(dto.getChannelType())) {
            if (!StringUtils.hasText(dto.getPlatformCode())) {
                throw new BizException("平台类渠道需填写平台编码");
            }
            String code = dto.getPlatformCode().trim().toUpperCase();
            if (!SUPPORTED_PLATFORMS.contains(code)) {
                throw new BizException("当前仅支持 Boss直聘(BOSS) 与 猎聘(LIEPIN)");
            }
        }
    }

    private String resolvePlatformCode(RecruitmentChannelCreateDTO dto) {
        if ("PLATFORM".equals(dto.getChannelType())) {
            return dto.getPlatformCode().trim().toUpperCase();
        }
        return null;
    }

    private void applySupportedPlatformScope(LambdaQueryWrapper<RecruitmentChannel> w) {
        w.in(RecruitmentChannel::getChannelCode, SUPPORTED_PLATFORMS);
    }

    private AccountCount countAccounts(Long tenantId, Long channelId) {
        AccountCount c = new AccountCount();
        LambdaQueryWrapper<AgentAccount> w = new LambdaQueryWrapper<>();
        w.eq(AgentAccount::getTenantId, tenantId).eq(AgentAccount::getChannelId, channelId);
        c.total = accountMapper.selectCount(w).intValue();
        w.eq(AgentAccount::getStatus, "ACTIVE");
        c.active = accountMapper.selectCount(w).intValue();
        return c;
    }

    private RecruitmentChannelVO toVO(RecruitmentChannel ch, AccountCount count) {
        RecruitmentChannelVO vo = new RecruitmentChannelVO();
        vo.setId(ch.getId());
        vo.setTenantId(ch.getTenantId());
        vo.setChannelCode(ch.getChannelCode());
        vo.setChannelName(ch.getChannelName());
        vo.setChannelType(ch.getChannelType());
        vo.setPlatformCode(ch.getPlatformCode());
        vo.setDescription(ch.getDescription());
        vo.setStatus(ch.getStatus());
        vo.setSortOrder(ch.getSortOrder());
        vo.setSupportsAgent(ch.getSupportsAgent() != null && ch.getSupportsAgent() == 1);
        vo.setSystem(ch.getIsSystem() != null && ch.getIsSystem() == 1);
        vo.setAccountCount(count.total);
        vo.setActiveAccountCount(count.active);
        vo.setCreatedAt(ch.getCreatedAt());
        vo.setUpdatedAt(ch.getUpdatedAt());
        return vo;
    }

    private static class AccountCount {
        int total;
        int active;
    }

    private static List<Object[]> defaultChannelRows() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"BOSS", "Boss直聘", "PLATFORM", "BOSS", "Boss直聘平台自动化寻源", "ACTIVE", 10, 1});
        rows.add(new Object[]{"LIEPIN", "猎聘", "PLATFORM", "LIEPIN", "猎聘平台自动化寻源", "ACTIVE", 20, 1});
        return rows;
    }
}
