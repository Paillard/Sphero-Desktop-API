/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.bin;

import org.freedesktop.DBus;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.types.DBusStructType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.*;

import static org.freedesktop.dbus.Gettext.getResource;
import static org.freedesktop.dbus.bin.IdentifierMangler.mangle;

/**
 * Converts a DBus XML file into Java interface definitions.
 */
public class CreateInterface
{
    @SuppressWarnings("unchecked")
    private static String collapseType(Type t, Set<String> imports, Map<StructStruct, Type[]> structs, boolean container, boolean fullnames) {
        if (t instanceof ParameterizedType) {
            String s;
            Class<?> c = (Class<?>) ((ParameterizedType) t).getRawType();
            if (null != structs && t instanceof DBusStructType) {
                int num = 1;
                String name = "Struct";
                while (null != structs.get(new StructStruct(name+num))) num++;
                name = name+num;
                structs.put(new StructStruct(name), ((ParameterizedType) t).getActualTypeArguments());
                return name;
            }
            if (null != imports) imports.add(c.getName());
            if (fullnames) return c.getName();
            else s = c.getSimpleName();
            s += '<';
            Type[] ts = ((ParameterizedType) t).getActualTypeArguments();
            for (Type st: ts)
                s += collapseType(st, imports, structs, true, fullnames)+',';
            s = s.replaceAll(",$", ">");
            return s;
        } else if (t instanceof Class) {
            Class<?> c = (Class<?>) t;
            if (c.isArray()) {
                return collapseType(c.getComponentType(), imports, structs, container, fullnames)+"[]";
            } else {
                Package p = c.getPackage();
                if (null != imports &&
                        !"java.lang".equals(p.getName())) imports.add(c.getName());
                if (container) {
                    if (fullnames) return c.getName();
                    else return c.getSimpleName();
                } else {
                    try {
                        Field f = c.getField("TYPE");
                        Class<?> d = (Class<?>) f.get(c);
                        return d.getSimpleName();
                    } catch (Exception e) {
                        return c.getSimpleName();
                    }
                }
            }
        } else return "";
    }
    private static String getJavaType(String dbus, Set<String> imports, Map<StructStruct,Type[]> structs, boolean container, boolean fullnames) throws DBusException
    {
        if (null == dbus || "".equals(dbus)) return "";
        Vector<Type> v = new Vector<>();
      /*int c =*/ Marshalling.getJavaType(dbus, v, 1);
        Type t = v.get(0);
        return collapseType(t, imports, structs, container, fullnames);
    }
    public String comment = "";
    boolean builtin;

    public CreateInterface(PrintStreamFactory factory, boolean builtin)
    {
        this.factory = factory;
        this.builtin = builtin;
    }
    @SuppressWarnings("fallthrough")
    String parseReturns(Vector<Element> out, Set<String> imports, Map<String,Integer> tuples, Map<StructStruct, Type[]> structs) throws DBusException
    {
        String[] names = { "Pair", "Triplet", "Quad", "Quintuple", "Sextuple", "Septuple" };
        String sig = "";
        String name = null;
        switch (out.size()) {
            case 0:
                sig += "void ";
                break;
            case 1:
                sig += getJavaType(out.get(0).getAttribute("type"), imports, structs, false, false)+" ";
                break;
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                name = names[out.size() - 2];
            default:
                if (null == name)
                    name = "NTuple"+out.size();

                tuples.put(name, out.size());
                sig += name + "<";
                for (Element arg: out)
                    sig += getJavaType(arg.getAttribute("type"), imports, structs, true, false)+", ";
                sig = sig.replaceAll(", $","> ");
                break;
        }
        return sig;
    }
    String parseMethod(Element meth, Set<String> imports, Map<String,Integer> tuples, Map<StructStruct, Type[]> structs, Set<String> exceptions, Set<String> anns) throws DBusException
    {
        Vector<Element> in = new Vector<>();
        Vector<Element> out = new Vector<>();
        if (null == meth.getAttribute("name") ||
                "".equals(meth.getAttribute("name"))) {
            System.err.println(getResource("ERROR: Method name was blank, failed"));
            System.exit(1);
        }
        String annotations = "";
        String throwses = null;

        for (Node a: new IterableNodeList(meth.getChildNodes())) {

            if (Node.ELEMENT_NODE != a.getNodeType()) continue;

            checkNode(a, "arg", "annotation");

            if ("arg".equals(a.getNodeName())) {
                Element arg = (Element) a;

                // methods default to in
                if ("out".equals(arg.getAttribute("direction")))
                    out.add(arg);
                else
                    in.add(arg);
            }
            else if ("annotation".equals(a.getNodeName())) {
                Element e = (Element) a;
                if (e.getAttribute("name").equals("org.freedesktop.DBus.Method.Error")) {
                    if (null == throwses)
                        throwses = e.getAttribute("value");
                    else
                        throwses += ", " + e.getAttribute("value");
                    exceptions.add(e.getAttribute("value"));
                } else
                    annotations += parseAnnotation(e, imports, anns);
            }
        }

        String sig = "";
        comment = "";
        sig += parseReturns(out, imports, tuples, structs);

        sig += mangle(meth.getAttribute("name"))+"(";

        char defaultname = 'a';
        String params = "";
        for (Element arg: in) {
            String type = getJavaType(arg.getAttribute("type"), imports, structs, false, false);
            String name = arg.getAttribute("name");
            if (null == name || "".equals(name)) name = ""+ defaultname++;
            params += type+" "+mangle(name)+", ";
        }
        return ("".equals(comment) ? "" : "   /**\n" + comment + "   */\n")
                + annotations + "  public " + sig +
                params.replaceAll("..$", "")+")"+
                (null == throwses? "": " throws "+throwses)+";";
    }
    String parseSignal(Element signal, Set<String> imports, Map<StructStruct, Type[]> structs, Set<String> anns) throws DBusException
    {
        Map<String, String> params = new HashMap<>();
        Vector<String> porder = new Vector<>();
        char defaultname = 'a';
        imports.add("org.freedesktop.dbus.DBusSignal");
        imports.add("org.freedesktop.dbus.exceptions.DBusException");
        String annotations = "";
        for (Node a: new IterableNodeList(signal.getChildNodes())) {

            if (Node.ELEMENT_NODE != a.getNodeType()) continue;

            checkNode(a, "arg", "annotation");

            if ("annotation".equals(a.getNodeName()))
                annotations += parseAnnotation((Element) a, imports, anns);
            else {
                Element arg = (Element) a;
                String type = getJavaType(arg.getAttribute("type"), imports, structs, false, false);
                String name = arg.getAttribute("name");
                if (null == name || "".equals(name)) name = ""+ defaultname++;
                params.put(mangle(name), type);
                porder.add(mangle(name));
            }
        }

        String out = "";
        out += annotations;
        out += "   public static class "+signal.getAttribute("name");
        out += " extends DBusSignal\n   {\n";
        for (String name: porder)
            out += "      public final "+params.get(name)+" "+name+";\n";
        out += "      public "+signal.getAttribute("name")+"(String path";
        for (String name: porder)
            out += ", "+params.get(name)+" "+name;
        out += ") throws DBusException\n      {\n         super(path";
        for (String name: porder)
            out += ", "+name;
        out += ");\n";
        for (String name: porder)
            out += "         this."+name+" = "+name+";\n";
        out += "      }\n";

        out += "   }\n";
        return out;
    }

    String parseAnnotation(Element ann, Set<String> imports, Set<String> annotations)
    {
        String s = "  @"+ann.getAttribute("name").replaceAll(".*\\.([^.]*)$","$1")+"(";
        if (null != ann.getAttribute("value")
                && !"".equals(ann.getAttribute("value")))
            s += '"'+ann.getAttribute("value")+'"';
        imports.add(ann.getAttribute("name"));
        annotations.add(ann.getAttribute("name"));
        s += ")\n";
        return s;
    }

    void parseInterface(Element iface, PrintStream out, Map<String,Integer> tuples, Map<StructStruct, Type[]> structs, Set<String> exceptions, Set<String> anns) throws DBusException
    {
        if (null == iface.getAttribute("name") ||
                "".equals(iface.getAttribute("name"))) {
            System.err.println(getResource("ERROR: Interface name was blank, failed"));
            System.exit(1);
        }

        out.println("package "+iface.getAttribute("name").replaceAll("\\.[^.]*$","")+";");

        String methods = "";
        String signals = "";
        String annotations = "";
        Set<String> imports = new TreeSet<>();
        imports.add("org.freedesktop.dbus.DBusInterface");
        for (Node meth: new IterableNodeList(iface.getChildNodes())) {

            if (Node.ELEMENT_NODE != meth.getNodeType()) continue;

            checkNode(meth, "method", "signal", "property", "annotation");

            if ("method".equals(meth.getNodeName()))
                methods += parseMethod((Element) meth, imports, tuples, structs, exceptions, anns) + "\n";
            else if ("signal".equals(meth.getNodeName()))
                signals += parseSignal((Element) meth, imports, structs, anns);
            else if ("property".equals(meth.getNodeName()))
                System.err.println("WARNING: Ignoring property");
            else if ("annotation".equals(meth.getNodeName()))
                annotations += parseAnnotation((Element) meth, imports, anns);
        }

        if (!imports.isEmpty())
            for (String i: imports)
                out.println("import "+i+";");

        out.print(annotations);
        out.print("public interface "+iface.getAttribute("name").replaceAll("^.*\\.([^.]*)$","$1"));
        out.println(" extends DBusInterface");
        out.println("{");
        out.println(signals);
        out.println(methods);
        out.println("}");
    }
    void createException(String name, String pack, PrintStream out) {
        out.println("package "+pack+";");
        out.println("import org.freedesktop.dbus.DBusExecutionException;");
        out.print("public class "+name);
        out.println(" extends DBusExecutionException");
        out.println("{");
        out.println("   public "+name+"(String message)");
        out.println("   {");
        out.println("      super(message);");
        out.println("   }");
        out.println("}");
    }
    void createAnnotation(String name, String pack, PrintStream out) {
        out.println("package "+pack+";");
        out.println("import java.lang.annotation.Retention;");
        out.println("import java.lang.annotation.RetentionPolicy;");
        out.println("@Retention(RetentionPolicy.RUNTIME)");
        out.println("public @interface "+name);
        out.println("{");
        out.println("   String value();");
        out.println("}");
    }
    void createStruct(String name, Type[] type, String pack, PrintStream out, Map<StructStruct, Type[]> existing) throws DBusException, IOException
    {
        out.println("package "+pack+";");

        Set<String> imports = new TreeSet<>();
        imports.add("org.freedesktop.dbus.Position");
        imports.add("org.freedesktop.dbus.Struct");
        Map<StructStruct, Type[]> structs = new HashMap<>(existing);
        String[] types = new String[type.length];
        for (int i = 0; i < type.length; i++)
            types[i] = collapseType(type[i], imports, structs, false, false);

        for (String im: imports) out.println("import "+im+";");

        out.println("public final class "+name+" extends Struct");
        out.println("{");
        int i = 0;
        char c = 'a';
        String params = "";
        for (String t: types) {
            out.println("   @Position("+i++ +")");
            out.println("   public final "+t+" "+c+";");
            params += t+" "+c+", ";
            c++;
        }
        out.println("  public "+name+"("+params.replaceAll("..$", "")+")");
        out.println("  {");
        for (char d = 'a'; d < c; d++)
            out.println("   this."+d+" = "+d+";");

        out.println("  }");
        out.println("}");

        structs = StructStruct.fillPackages(structs, pack);
        Map<StructStruct, Type[]> tocreate = new HashMap<>(structs);
        existing.keySet().forEach(tocreate::remove);
        createStructs(tocreate, structs);
    }
    void createTuple(String name, int num, String pack, PrintStream out) {
        out.println("package "+pack+";");
        out.println("import org.freedesktop.dbus.Position;");
        out.println("import org.freedesktop.dbus.Tuple;");
        out.println("/** Just a typed container class */");
        out.print("public final class "+name);
        String types = " <";
        for (char v = 'A'; v < 'A'+num; v++)
            types += v + ",";
        out.print(types.replaceAll(",$","> "));
        out.println("extends Tuple");
        out.println("{");

        char t = 'A';
        char n = 'a';
        for (int i = 0; i < num; i++,t++,n++) {
            out.println("   @Position("+i+")");
            out.println("   public final "+t+" "+n+";");
        }

        out.print("   public "+name+"(");
        String sig = "";
        t = 'A';
        n = 'a';
        for (int i = 0; i < num; i++,t++,n++)
            sig += t+" "+n+", ";
        out.println(sig.replaceAll(", $", ")"));
        out.println("   {");
        for (char v = 'a'; v < 'a'+num; v++)
            out.println("      this."+v+" = "+v+";");
        out.println("   }");

        out.println("}");
    }
    void parseRoot(Element root) throws DBusException, IOException
    {
        Map<StructStruct, Type[]> structs = new HashMap<>();
        Set<String> exceptions = new TreeSet<>();
        Set<String> annotations = new TreeSet<>();

        for (Node iface: new IterableNodeList(root.getChildNodes())) {

            if (Node.ELEMENT_NODE != iface.getNodeType()) continue;

            checkNode(iface, "interface", "node");

            if ("interface".equals(iface.getNodeName())) {

                Map<String, Integer> tuples = new HashMap<>();
                String name = ((Element) iface).getAttribute("name");
                String file = name.replaceAll("\\.","/")+".java";
                String path = file.replaceAll("/[^/]*$", "");
                String pack = name.replaceAll("\\.[^.]*$","");

                // don't create interfaces in org.freedesktop.DBus by default
                if (pack.startsWith("org.freedesktop.DBus") && !builtin) continue;

                factory.init(file, path);
                parseInterface((Element) iface,
                        factory.createPrintStream(file), tuples, structs, exceptions, annotations);

                structs = StructStruct.fillPackages(structs, pack);
                createTuples(tuples, pack);
            }
            else if ("node".equals(iface.getNodeName()))
                parseRoot((Element) iface);
            else {
                System.err.println(getResource("ERROR: Unknown node: ")+iface.getNodeName());
                System.exit(1);
            }
        }

        createStructs(structs, structs);
        createExceptions(exceptions);
        createAnnotations(annotations);
    }
    private void createAnnotations(Set<String> annotations) throws DBusException, IOException
    {
        for (String fqn: annotations) {
            String name = fqn.replaceAll("^.*\\.([^.]*)$", "$1");
            String pack = fqn.replaceAll("\\.[^.]*$","");
            // don't create things in org.freedesktop.DBus by default
            if (pack.startsWith("org.freedesktop.DBus") && !builtin)
                continue;
            String path = pack.replaceAll("\\.", "/");
            String file = name.replaceAll("\\.","/")+".java";
            factory.init(file, path);
            createAnnotation(name, pack,
                    factory.createPrintStream(path, name));
        }
    }
    private void createExceptions(Set<String> exceptions) throws DBusException, IOException
    {
        for (String fqn: exceptions) {
            String name = fqn.replaceAll("^.*\\.([^.]*)$", "$1");
            String pack = fqn.replaceAll("\\.[^.]*$","");
            // don't create things in org.freedesktop.DBus by default
            if (pack.startsWith("org.freedesktop.DBus") && !builtin)
                continue;
            String path = pack.replaceAll("\\.", "/");
            String file = name.replaceAll("\\.","/")+".java";
            factory.init(file, path);
            createException(name, pack,
                    factory.createPrintStream(path, name));
        }
    }
    private void createStructs(Map<StructStruct, Type[]> structs, Map<StructStruct, Type[]> existing) throws DBusException, IOException
    {
        for (StructStruct ss: structs.keySet())  {
            String file = ss.name.replaceAll("\\.","/")+".java";
            String path = ss.pack.replaceAll("\\.", "/");
            factory.init(file, path);
            createStruct(ss.name, structs.get(ss), ss.pack,
                    factory.createPrintStream(path, ss.name), existing);
        }
    }

    private void createTuples(Map<String, Integer> typeMap, String pack) throws DBusException, IOException
    {
        for (String tname: typeMap.keySet())
            createTuple(tname, typeMap.get(tname), pack,
                    factory.createPrintStream(pack.replaceAll("\\.", "/"), tname));
    }

    public abstract static class PrintStreamFactory
    {

        public abstract void init(String file, String path);

        /**
         * @param path
         * @param tname
         * @return PrintStream
         * @throws IOException
         */
        public PrintStream createPrintStream(String path, String tname) throws IOException
        {
            String file = path+"/"+tname+".java";
            return createPrintStream(file);
        }

        /**
         * @param file
         * @return PrintStream
         * @throws IOException
         */
        public abstract PrintStream createPrintStream(String file) throws IOException;

    }
    static class ConsoleStreamFactory extends CreateInterface.PrintStreamFactory
    {

        @Override
        public
        void init(String file, String path)
        {
        }

        @Override
        public
        PrintStream createPrintStream(String file) throws IOException
        {
            System.out.println("/* File: "+file+" */");
            return System.out;
        }

        public PrintStream createPrintStream(String path, String tname) throws IOException
        {
            return super.createPrintStream(path, tname);
        }

    }

    static class FileStreamFactory extends CreateInterface.PrintStreamFactory
    {
        public void init(String file, String path)
        {
            new File(path).mkdirs();
        }


        /**
         * @param file
         * @return
         * @throws IOException
         */
        public PrintStream createPrintStream(String file) throws IOException
        {
            return new PrintStream(new FileOutputStream(file));
        }

    }

    static void checkNode(Node n, String... names)
    {
        String expected = "";
        for (String name: names) {
            if (name.equals(n.getNodeName())) return;
            expected += name + " or ";
        }
        System.err.println(MessageFormat.format(getResource("ERROR: Expected {0}, got {1}, failed."), new Object[] { expected.replaceAll("....$", ""), n.getNodeName() }));
        System.exit(1);
    }

    private final CreateInterface.PrintStreamFactory factory;

    static class Config
    {
        int bus = DBusConnection.SESSION;
        String busname;
        String object;
        File datafile;
        boolean printtree;
        boolean fileout;
        boolean builtin;
    }

    static void printSyntax()
    {
        printSyntax(System.err);
    }
    static void printSyntax(PrintStream o)
    {
        o.println("Syntax: CreateInterface <options> [file | busname object]");
        o.println("        Options: --no-ignore-builtin --system -y --session -s --create-files -f --help -h --version -v");
    }
    public static void version()
    {
        System.out.println("Java D-Bus Version "+System.getProperty("Version"));
        System.exit(1);
    }

    static CreateInterface.Config parseParams(String... args)
    {
        CreateInterface.Config config = new CreateInterface.Config();
        for (String p: args) {
            if ("--system".equals(p) || "-y".equals(p))
                config.bus = DBusConnection.SYSTEM;
            else if ("--session".equals(p) || "-s".equals(p))
                config.bus = DBusConnection.SESSION;
            else if ("--no-ignore-builtin".equals(p))
                config.builtin = true;
            else if ("--create-files".equals(p) || "-f".equals(p))
                config.fileout = true;
            else if ("--print-tree".equals(p) || "-p".equals(p))
                config.printtree = true;
            else if ("--help".equals(p) || "-h".equals(p)) {
                printSyntax(System.out);
                System.exit(0);
            } else if ("--version".equals(p) || "-v".equals(p)) {
                version();
                System.exit(0);
            } else if (p.startsWith("-")) {
                System.err.println(getResource("ERROR: Unknown option: ")+p);
                printSyntax();
                System.exit(1);
            }
            else {
                if (null == config.busname) config.busname = p;
                else if (null == config.object) config.object = p;
                else {
                    printSyntax();
                    System.exit(1);
                }
            }
        }
        if (null == config.busname) {
            printSyntax();
            System.exit(1);
        }
        else if (null == config.object) {
            config.datafile = new File(config.busname);
            config.busname = null;
        }
        return config;
    }

    public static void main(String... args) throws Exception
    {
        CreateInterface.Config config = parseParams(args);

        Reader introspectdata = null;

        if (null != config.busname) try {
            DBusConnection conn = DBusConnection.getConnection(config.bus);
            DBus.Introspectable in = conn.getRemoteObject(config.busname, config.object, DBus.Introspectable.class);
            String id = in.Introspect();
            if (null == id) {
                System.err.println(getResource("ERROR: Failed to get introspection data"));
                System.exit(1);
            }
            introspectdata = new StringReader(id);
            conn.disconnect();
        } catch (DBusException | DBusExecutionException DBe) {
            System.err.println(getResource("ERROR: Failure in DBus Communications: ")+DBe.getMessage());
            System.exit(1);
        }
        else if (null != config.datafile) try {
            introspectdata = new InputStreamReader(new FileInputStream(config.datafile));
        } catch (FileNotFoundException FNFe) {
            System.err.println(getResource("ERROR: Could not find introspection file: ")+FNFe.getMessage());
            System.exit(1);
        }
        try {
            CreateInterface.PrintStreamFactory factory = config.fileout  ? new CreateInterface.FileStreamFactory() : new CreateInterface.ConsoleStreamFactory();
            CreateInterface createInterface = new CreateInterface(factory, config.builtin);
            createInterface.createInterface(introspectdata);
        } catch (DBusException DBe) {
            System.err.println("ERROR: "+DBe.getMessage());
            System.exit(1);
        }
    }
    /** Output the interface for the supplied xml reader
     * @param introspectdata The introspect data reader
     * @throws ParserConfigurationException If the xml parser could not be configured
     * @throws SAXException If a problem occurs reading the xml data
     * @throws IOException If an IO error occurs
     * @throws DBusException If the dbus related error occurs
     */
    public void createInterface(Reader introspectdata) throws ParserConfigurationException, SAXException, IOException, DBusException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(introspectdata));

        Element root = document.getDocumentElement();
        checkNode(root, "node");
        parseRoot(root);

    }
}


