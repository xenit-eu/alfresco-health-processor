package eu.xenit.alfresco.healthprocessor.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.alfresco.service.namespace.InvalidQNameException;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ParameterCheck;
import org.springframework.util.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QNameUtil {

    private static final String QNAME_REGEX = "(\\{.+}.+)";
    private static final Pattern QNAME_PATTERN = Pattern.compile(QNAME_REGEX, Pattern.DOTALL);

    public static QName toQName(String qName, NamespacePrefixResolver namespacePrefixResolver)
            throws NullPointerException, InvalidQNameException {
        ParameterCheck.mandatory("qName", qName);

        qName = qName.trim();

        if (QNAME_PATTERN.matcher(qName).matches()) {
            return QName.createQName(qName);
        } else {
            return QName.createQName(qName, namespacePrefixResolver);
        }
    }

    public static Set<QName> toQNames(Collection<String> qNames, NamespacePrefixResolver namespacePrefixResolver) {
        Set<QName> ret = new HashSet<>(qNames.size());

        for (String qName : qNames) {
            if (StringUtils.hasText(qName)) {
                ret.add(toQName(qName, namespacePrefixResolver));
            }
        }

        return ret;
    }
}
