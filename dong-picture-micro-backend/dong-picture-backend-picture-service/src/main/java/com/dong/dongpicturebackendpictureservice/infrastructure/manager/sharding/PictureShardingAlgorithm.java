package com.dong.dongpicturebackendpictureservice.infrastructure.manager.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

/**
 * @author by hongdou
 * @date 2025/11/12.
 * @DESC:  图片分表算法
 */
public class PictureShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    /**
     *
     * @param availableTargetNames 这个是由配置文件中的actual-data-nodes指定的，但是我们配置中指定的固定picture，
     *                             需要在运行时根据spaceId动态生成分表名
     * @param preciseShardingValue
     * @return
     */
    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> preciseShardingValue) {
        // 分表策略：根据spaceId取模
        Long spaceId = preciseShardingValue.getValue();
        String logicTableName = preciseShardingValue.getLogicTableName();
        // spaceId为null表示查询所有图片
        if (spaceId == null) {
            return logicTableName;
        }
        // 根据spaceId动态生成分表名
        String realTableName = "picture_" + spaceId;
        if (availableTargetNames.contains(realTableName)) {
            return realTableName;
        }else {
            return logicTableName;
        }
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Long> rangeShardingValue) {
        return new ArrayList<>();
    }

    @Override
    public Properties getProps() {
        return null;
    }

    @Override
    public void init(Properties properties) {}
}
