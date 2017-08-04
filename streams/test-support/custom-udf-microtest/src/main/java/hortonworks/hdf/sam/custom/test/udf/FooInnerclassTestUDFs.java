package hortonworks.hdf.sam.custom.test.udf;

import com.hortonworks.streamline.streams.rule.UDF;
import com.hortonworks.streamline.streams.rule.UDF2;

/**
 *
 */
public class FooInnerclassTestUDFs {

  static public class FooAdder implements UDF2<Integer, Integer, Integer> {
    @Override
    public Integer evaluate(Integer a, Integer b) {
      return a + b;
    }
  }

  static public class FooPredicateGTZ implements UDF<Boolean, Integer> {
    @Override
    public Boolean evaluate(Integer i) {
      return i > 0;
    }
  }

}
