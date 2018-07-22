package sanitize;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

public class owaspJavaHtmlSanitizerSanitize {

    public static void main(String[] args) {
        if (args.length > 0) {
            PolicyFactory policy = new HtmlPolicyBuilder()
                    .allowElements("a")
                    .allowAttributes("href").onElements("a")
                    .requireRelNofollowOnLinks()
                    .toFactory();
            System.out.print("Result:"+policy.sanitize(args[0]));
        }
    }
}
