package eu.xenit.alfresco.healthprocessor.util;

import java.util.Collection;
import java.util.Iterator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringUtil {

    public static String join(Collection<?> collection, String delimiter) {
        if (collection == null) {
            return null;
        }

        final StringBuilder retBuilder = new StringBuilder();
        Iterator<?> it = collection.iterator();

        while (it.hasNext()) {
            retBuilder.append(it.next().toString());
            if (it.hasNext()) {
                retBuilder.append(delimiter);
            }
        }

        return retBuilder.toString();
    }


}
