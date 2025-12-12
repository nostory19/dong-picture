package com.dong.dongpicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dong.dongpicturebackend.exception.BusinessException;
import com.dong.dongpicturebackend.exception.ErrorCode;
import com.dong.dongpicturebackend.exception.ThrowUtils;
import com.dong.dongpicturebackend.mapper.SpaceMapper;
import com.dong.dongpicturebackend.model.dto.space.analyze.*;
import com.dong.dongpicturebackend.model.entity.Picture;
import com.dong.dongpicturebackend.model.entity.Space;
import com.dong.dongpicturebackend.model.entity.User;
import com.dong.dongpicturebackend.model.vo.space.analyze.*;
import com.dong.dongpicturebackend.service.PictureService;
import com.dong.dongpicturebackend.service.SpaceAnalyzeService;
import com.dong.dongpicturebackend.service.SpaceService;
import com.dong.dongpicturebackend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author by hongdou
 * @date 2025/10/12.
 * @DESC: 空间分析服务实现
 */
@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceAnalyzeService {
    //    private final UserService userService;
//
//    public SpaceAnalyzeServiceImpl(UserService userService) {
//        this.userService = userService;
//    }
    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;
//其中公共函数，包含校验空间权限、根据分析范围填充查询对象

    @Resource
    private PictureService pictureService;




    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        // 判断是否为空
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 根据请求参数，判断是管理员还是私有空间权限
        if (spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()) {
            boolean isAdmin = userService.isAdmin(loginUser);
            ThrowUtils.throwIf(!isAdmin, ErrorCode.NOT_AUTH_ERROR, "无权访问空间");
            // 如果是管理员，查询所有空间
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            // 如果是查询公共图库
            if (!spaceUsageAnalyzeRequest.isQueryAll()) {
                queryWrapper.isNull("spaceId");
            }
            // ======获得查询结果======-
            // 不用list，因为只需要计算总大小和数量，如果使用list会占用大量内存
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            // 计算使用大小，把Object转换为Long，然后统计和
            // 也可以在数据库中计算总和，但是为了统一逻辑，在这里计算
            long usedSize = pictureObjList.stream().mapToLong(result -> result instanceof Long ? (Long) result : 0).sum();
            // 图片条数
            long usedCount = pictureObjList.size();
            // 封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            // 设置使用大小和图片条数
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            // 由于是公共图库或者所有空间，剩余空间和总空间设置为无上限，无比例
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            return spaceUsageAnalyzeResponse;
        } else {
            // 私有空间，检查权限
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            // 查询空间是否存在
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 判断是否是自己的空间
            spaceService.checkSpaceAuth(space, loginUser);
            // 构造返回结果
            SpaceUsageAnalyzeResponse response = new SpaceUsageAnalyzeResponse();
            response.setUsedSize(space.getTotalSize());
            response.setMaxSize(space.getMaxSize());
            // 计算使用比例
            // 保留两位小数
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            response.setSizeUsageRatio(sizeUsageRatio);
            response.setUsedCount(space.getTotalCount());
            response.setMaxCount(space.getMaxCount());
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            response.setCountUsageRatio(countUsageRatio);
            return response;

        }
    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        // 先判断是否为空
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 校验权限
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        // 构造查询条件，查询分类和数量
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 判断查询范围
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        // 实现分组查询
        queryWrapper.select("category", "COUNT(*) as count", "SUM(picSize) as totalSize")
                .groupBy("category");
        // 执行查询
        // 然后将map转换为List<SpaceCategoryAnalyzeResponse>
        // 例如(Long) result.get("count")是BigDecimal，不能直接转换，
        // 需要先转换为Number，然后再转换为Long
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String category = result.get("category") != null ? result.get("category").toString() : "未分类";
                    Long count = ((Number) result.get("count")).longValue();
                    Long totalSize = ((Number) result.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                })
                .collect(Collectors.toList());

    }

    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        // 先判断是否为空
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 校验权限
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);
        // 查询，注意标签是字符串数组，不能直接通过sql查询出来
        // 先查询出所有符合条件的标签，只需要tags这一个字段
        queryWrapper.select("tags");
        // 然后执行查询，过滤掉没有标签的字段，映射到string
        List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());
        // 解析标签，统计标签出现的次数
        // 先扁平化，然后分组统计
        Map<String, Long> tagCountMap = tagsJsonList.stream()
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        // 封装为响应对象
        // 按照使用次数排序
        return tagCountMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        // 先判断是否为空
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 校验权限
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);
        // 查询所有符合条件的图片大小
        // map转换，将size转化为数字类型
        // picSizes列表为[20,1024,,,]等代表图片大小的
        queryWrapper.select("picSize");
        List<Long> picSizes = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .map(size -> ((Number) size).longValue())
                .collect(Collectors.toList());
        // 自定义分段范围，使用有序Map，这里可以用TreeMap,LinkedMap
        LinkedHashMap<String, Long> sizeRanges = new LinkedHashMap<>();
//        // 用字符串定义范围，然后使用filter进行过滤
//        sizeRanges.put("<100KB", picSizes.stream().filter(size -> size < 100 * 1024).count());
//        sizeRanges.put("100KB-500KB", picSizes.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
//        sizeRanges.put("500KB-1MB", picSizes.stream().filter(size -> size >= 500 * 1024 && size < 1 * 1024 * 1024).count());
//        sizeRanges.put(">1MB", picSizes.stream().filter(size -> size >= 1* 1024 * 1024).count());

        // 对上述流式处理进行优化，遍历一次picSizes，进行分段统计
        long less100KB = 0, between100KBAnd500KB = 0, between500KBAnd1MB = 0, greater1MB = 0;
        for (Long size : picSizes){
            if (size < 100 * 1024){
                less100KB++;
            }else if (size < 500 * 1024){
                between100KBAnd500KB++;
            }else if (size < 1* 1024 * 1024){
                between500KBAnd1MB++;
            }else{
                greater1MB++;
            }
        }
        // 然后就可以直接放入map
        sizeRanges.put("<100KB", less100KB);
        sizeRanges.put("100KB-500KB", between100KBAnd500KB);
        sizeRanges.put("500KB-1MB", between500KBAnd1MB);
        sizeRanges.put(">1MB", greater1MB);

        // 封装为响应对象
        return sizeRanges.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 判断图片上传的用户id是否为空
        Long userId = spaceUserAnalyzeRequest.getUserId();
        // 注意这里的userId使用的是请求的userId，而不是登录用户
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);

        // 查询，根据请求的时间维度进行查询
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension){
            case "day":
                // 按天统计，格式化为yyyy-MM-dd
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') as period", "COUNT(*) as count")
                        .groupBy("period")
                        .orderByAsc("period");
                break;
            case "month":
                // 按月统计，格式化为yyyy-MM
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') as period", "COUNT(*) as count")
                        .groupBy("period")
                        .orderByAsc("period");
                break;
            case "week":
                // 按周统计，格式化为yyyy-ww
                queryWrapper.select("YEARWEEK(createTime) as period", "COUNT(*) as count")
                        .groupBy("period")
                        .orderByAsc("period");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");

        }

        // 查询
        List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);
        // 封装为响应对象
        // 这里也是同样用到了Number进行转换
        return queryResult.stream()
                .map(result -> {
                    String period = result.get("period").toString();
                    Long count = ((Number) result.get("count")).longValue();
                    return new SpaceUserAnalyzeResponse(period, count);
                })
                .collect(Collectors.toList());

    }

    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 只有管理员可以访问
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NOT_AUTH_ERROR, "无权访问空间排行");
        // 查询所有空间，按照图片数量排序，取前N名
//        int topN = spaceRankAnalyzeRequest.getTopN() != null && spaceRankAnalyzeRequest.getTopN() > 0 ? spaceRankAnalyzeRequest.getTopN() : 10;
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalSize")
                .last("LIMIT " + spaceRankAnalyzeRequest.getTopN());

        return spaceService.list(queryWrapper);
    }


    /**
     * 校验空间权限
     *
     * @param spaceAnalyzeRequest
     * @param loginUser
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        // 取出参数

        // 判断是管理员还是私有空间权限
        // 是管理员
        if (spaceAnalyzeRequest.isQueryAll() || spaceAnalyzeRequest.isQueryPublic()) {
            // 校验是否是管理员，即全空间或者公共图库分析判断是否为管理员
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NOT_AUTH_ERROR, "无权访问公共图库`");

        } else {
            // 判断是否是私有空间
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            // 查询空间是否存在
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 判断是否是自己的空间
            // 和spaceService中通用方法一样，校验权限
            spaceService.checkSpaceAuth(space, loginUser);
        }
    }

    /**
     * 根据分析范围填充查询对象
     *
     * @param spaceAnalyzeRequest
     * @param queryWrapper
     */
    private static void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        // 针对三种情况queryAll, queryPublic, spaceId进行填充
        if (spaceAnalyzeRequest.isQueryAll()) {
            return;
        }
        if (spaceAnalyzeRequest.isQueryPublic()) {
            // 公共图库的spaceId为空
            queryWrapper.isNull("spaceId");
            return;
        }
        // 私有空间
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }
}
