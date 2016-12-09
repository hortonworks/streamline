package org.apache.streamline.common;


import java.util.ArrayList;
import java.util.List;

public  class QueryParam {

    public final String name;
    public final String value;
    public QueryParam(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryParam that = (QueryParam) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return !(value != null ? !value.equals(that.value) : that.value != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    public static List<QueryParam> params(String... args) {
        List<QueryParam> queryParams = new ArrayList<>();
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Expects even number of arguments");
        }
        for (int i = 0; i < args.length; i += 2) {
            queryParams.add(new QueryParam(args[i], args[i + 1]));
        }
        return queryParams;
    }
}
