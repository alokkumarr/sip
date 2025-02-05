package synchronoss.spark.functions.rt;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.function.FlatMapFunction;
import scala.Tuple2;
import synchronoss.data.countly.model.CountlyModel;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by asor0002 on 11/4/2016.
 */
public class TransformCountlyRecord implements FlatMapFunction<Iterator<ConsumerRecord<String, String>>, String> {

    private static final Logger logger = Logger.getLogger(TransformCountlyRecord.class);
    private static final long serialVersionUID = 4019208537608178076L;

    public Iterator<String> call(Iterator<ConsumerRecord<String, String>> in) throws Exception {
        List<String> ret = new ArrayList<>();

        while (in.hasNext()) {
            String src = in.next().value();
            if (src!= null && !src.isEmpty()) {
                try {
                    String transformedRecord = CountlyModel.transform(src);
                    ret.add(transformedRecord);
                } catch (com.google.gson.stream.MalformedJsonException e){
                    logger.error("Malformed JSON");
                    logger.error(e.getMessage());
                    logger.error(src);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    logger.error(src);
                }
            }
        }
        return ret.iterator();
    }
}
