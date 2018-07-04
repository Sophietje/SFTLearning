import org.apache.commons.lang3.StringEscapeUtils;

public class EscapeHTML {

    public static void main(String[] args) {
        if (args.length > 0) {
            System.out.println(StringEscapeUtils.escapeHtml3(args[0]));
        }
    }
}