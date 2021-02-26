package eu.xenit.alfresco.healthprocessor.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SetUtil {

    public static <T> Set<T> set(T... values) {
        return new HashSet<>(Arrays.asList(values));
    }

}
