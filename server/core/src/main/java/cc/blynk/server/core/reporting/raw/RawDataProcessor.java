package cc.blynk.server.core.reporting.raw;

import cc.blynk.server.core.reporting.average.AggregationKey;
import cc.blynk.utils.NumberUtil;
import cc.blynk.utils.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simply stores every record in memory that should be stored in reporting DB lately.
 * Could cause OOM at high request rate. However we don't use it very high loads.
 * So this is fine for now.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.01.17.
 */
public class RawDataProcessor {

    public final Map<AggregationKey, Object> rawStorage;

    public RawDataProcessor(boolean enable) {
        if (enable) {
            rawStorage = new ConcurrentHashMap<>();
        } else {
            rawStorage = Collections.emptyMap();
        }
    }

    //todo 2 millis is minimum allowed interval for data pushing.
    public void collect(BaseReportingKey key, long ts, String stringValue, double doubleValue) {
        final AggregationKey aggregationKey = new AggregationKey(key, ts);
        if (doubleValue == NumberUtil.NO_RESULT) {
            //storing for now just first part
            String part1 = stringValue.split(StringUtils.BODY_SEPARATOR_STRING)[0];
            rawStorage.put(aggregationKey, part1);
        } else {
            rawStorage.put(aggregationKey, doubleValue);
        }
    }

    public void collect(BaseReportingKey key, long ts, String stringValue) {
        rawStorage.put(new AggregationKey(key, ts), stringValue);
    }

    public void collect(BaseReportingKey key, long ts, double doubleValue) {
        rawStorage.put(new AggregationKey(key, ts), doubleValue);
    }

}
