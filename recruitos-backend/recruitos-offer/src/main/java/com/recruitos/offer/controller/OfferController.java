package com.recruitos.offer.controller;

import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import com.recruitos.offer.dto.*;
import com.recruitos.offer.service.OfferService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * Offer management controller
 */
@Api(tags = "Offer Management")
@RestController
@RequestMapping("/api/offer")
public class OfferController {

    @Resource
    private OfferService offerService;

    @ApiOperation("Create an offer")
    @PostMapping
    public R<OfferVO> createOffer(@RequestBody OfferCreateDTO dto) {
        OfferVO vo = offerService.createOffer(dto);
        return R.ok(vo);
    }

    @ApiOperation("Submit offer for approval")
    @PostMapping("/{id}/submit")
    public R<OfferVO> submitApproval(@PathVariable Long id) {
        OfferVO vo = offerService.submitApproval(id);
        return R.ok(vo);
    }

    @ApiOperation("Approve or reject an offer")
    @PostMapping("/{id}/approve")
    public R<OfferVO> approve(@PathVariable Long id, @RequestBody OfferApprovalDTO dto) {
        OfferVO vo = offerService.approve(id, dto);
        return R.ok(vo);
    }

    @ApiOperation("Send an approved offer")
    @PostMapping("/{id}/send")
    public R<OfferVO> sendOffer(@PathVariable Long id) {
        OfferVO vo = offerService.sendOffer(id);
        return R.ok(vo);
    }

    @ApiOperation("Accept an offer")
    @PostMapping("/{id}/accept")
    public R<OfferVO> acceptOffer(@PathVariable Long id) {
        OfferVO vo = offerService.acceptOffer(id);
        return R.ok(vo);
    }

    @ApiOperation("Reject an offer")
    @PostMapping("/{id}/reject")
    public R<OfferVO> rejectOffer(@PathVariable Long id) {
        OfferVO vo = offerService.rejectOffer(id);
        return R.ok(vo);
    }

    @ApiOperation("Get offer list with pagination")
    @GetMapping("/list")
    public R<PageResult<OfferVO>> getOfferList(OfferQueryDTO query) {
        PageResult<OfferVO> result = offerService.getOfferList(query);
        return R.ok(result);
    }

    @ApiOperation("Get offer detail")
    @GetMapping("/{id}")
    public R<OfferVO> getOfferDetail(@PathVariable Long id) {
        OfferVO vo = offerService.getOfferDetail(id);
        return R.ok(vo);
    }
}
