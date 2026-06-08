package com.recruitos.tenant.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.recruitos.common.auth.CurrentUser;
import com.recruitos.common.result.R;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.tenant.entity.Notification;
import com.recruitos.tenant.mapper.NotificationMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Api(tags = "Notification")
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationMapper notificationMapper;

    @ApiOperation("My notifications")
    @GetMapping("/my")
    public R<List<Notification>> myNotifications(@RequestParam(defaultValue = "20") int limit) {
        Long userId = CurrentUser.getCurrentUserId();
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getTenantId, tenantId)
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt)
                .last("LIMIT " + limit);
        return R.ok(notificationMapper.selectList(wrapper));
    }

    @ApiOperation("Mark notification as read")
    @PutMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getId, id)
                .set(Notification::getIsRead, 1)
                .set(Notification::getReadAt, LocalDateTime.now());
        notificationMapper.update(null, wrapper);
        return R.ok();
    }
}
