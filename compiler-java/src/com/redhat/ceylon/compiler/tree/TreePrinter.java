package com.redhat.ceylon.compiler.tree;

import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import com.sun.tools.javac.util.List;

public class TreePrinter extends CeylonTree.Visitor {
    private PrintWriter out;

    public TreePrinter(Writer out) {
        this.out = new PrintWriter(out);
    }

    private int depth;

    private void indent() {
        out.println();
        for (int i = 0; i < depth; i++)
            out.print("  ");
    }

    private static class NameValuePair implements Comparable {
        public String name;
        public Object value;

        public NameValuePair(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public int compareTo(Object o) {
            NameValuePair that = (NameValuePair) o;
            boolean thisIsName = this.name.equals("name");
            boolean thatIsName = that.name.equals("name");
            if (thisIsName) {
                if (thatIsName)
                    return 0;
                return -1;
            }
            if (thatIsName)
                return 1;
            return this.name.compareTo(that.name);
        }
    }

    private List<NameValuePair> getFields(CeylonTree tree) {
        return getFields(tree.getClass(), tree);
    }

    private List<NameValuePair> getFields(Class klass, CeylonTree tree) {
        List<NameValuePair> result;
        if (klass == CeylonTree.class)
            result = List.<NameValuePair>nil();
        else
            result = getFields(klass.getSuperclass(), tree);

        List<NameValuePair> tmpL = List.<NameValuePair>nil();
        for (Field field: klass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()))
                continue;

            String name = field.getName();
            if (name.equals("parent") || name.equals("children"))
                continue;
            if (name.equals("token"))
                continue;

            Object value;
            try {
                value = field.get(tree);
            }
            catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            tmpL = tmpL.append(new NameValuePair(name, value));
        }

        NameValuePair[] tmpA = new NameValuePair[tmpL.size()];
        tmpL.toArray(tmpA);
        Arrays.sort(tmpA);

        return result.appendList(List.<NameValuePair>from(tmpA));
    }

    private void enter(String what) {
        indent();
        out.print("(" + what);
        depth++;
    }
    private void leave() {
        depth--;
        out.print(")");
    }

    public void visitDefault(CeylonTree tree) {
        enter(tree.getClassName());
        for (NameValuePair field: getFields(tree)) {
            Object value = field.value;
            if (value == null)
                continue;

            enter(field.name);
            if (value instanceof String) {
                out.print(" \"" + value + "\"");
            }
            else if (value instanceof CeylonTree) {
                ((CeylonTree) value).accept(this);
            }
            else if (value instanceof List) {
                for (Object child: (List) value) {
                    ((CeylonTree) child).accept(this);
                }
            }
            else {
                throw new RuntimeException(value.getClass().getName());
            }
            leave();
        }
        leave();
    }
}
