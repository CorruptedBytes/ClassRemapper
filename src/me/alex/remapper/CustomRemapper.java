package me.alex.remapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

public class CustomRemapper {

    public static final Map <String, byte[]> files = new HashMap<>();
    public static final List <ClassNode> classes = new ArrayList<>();

    public final static byte DESC_VOID = 0;
    public final static byte DESC_ARRAY_STRING = 102;


    protected static void remapClass(List<ClassNode> classMap) {
        Map<String, String> remap = new HashMap<>();

        List <String> keys = classMap.stream().map(c -> c.name).collect(Collectors.toList());
        Collections.shuffle(keys);
        for (String key: keys) {
            ClassNode cn = getClassNode(key, classMap);
            remap.put(cn.name, getRandomName());
        }

        applyMappings(classMap, remap);
    }
    
    @SuppressWarnings("unused")
	protected static void remapFields(List<ClassNode> classMap) {
        Map<String, String> remap = new HashMap<>();

        List<String> keys = classMap.stream().map(c -> c.name).collect(Collectors.toList());
        Collections.shuffle(keys);
        for (String key : keys) {
       	 ClassNode cn = getClassNode(key, classMap);
       	 remap.put(cn.name, getRandomName());
        }
   
     List<FieldNode> fields = new ArrayList<>();
        classMap.forEach(c -> fields.addAll(c.fields));
        Collections.shuffle(fields);
        int i = 0;
        for (FieldNode f : fields) {
            Stack<ClassNode> nodeStack = new Stack<>();
            nodeStack.add(getOwner(f, classMap));
            while (nodeStack.size() > 0) {
                ClassNode node = nodeStack.pop();
                remap.put(node.name + "." + f.name, getRandomName());
                nodeStack.addAll(classMap.stream().
                        filter(cn -> cn.superName.equals(node.name)).
                        collect(Collectors.toList()));
            }
            i++;
        }
        

        applyMappings(classMap, remap);
    }

    protected static void saveJar(File out) {
        try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(out))) {
            for (ClassNode classNode: CustomRemapper.classes) {
                jarOutputStream.putNextEntry(new JarEntry(classNode.name + ".class"));
                jarOutputStream.write(CustomRemapper.toByteArray(classNode));
                jarOutputStream.closeEntry();
            }

            for (Map.Entry <String, byte[]> entry: CustomRemapper.files.entrySet()) {
                jarOutputStream.putNextEntry(new JarEntry(entry.getKey()));
                jarOutputStream.write(entry.getValue());
                jarOutputStream.closeEntry();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static void loadJar(File file) {
        try (JarFile jarFile = new JarFile(file)) {
            final Enumeration <JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                final JarEntry jarEntry = entries.nextElement();
                try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                    final byte[] bytes = IOUtils.toByteArray(inputStream);
                    if (!jarEntry.getName().endsWith(".class")) {
                        files.put(jarEntry.getName(), bytes);
                        continue;
                    }

                    try {
                        if (checkClassVerify(bytes)) {
                            final ClassNode classNode = new ClassNode();
                            new ClassReader(bytes).accept(classNode, ClassReader.EXPAND_FRAMES);
                            CustomRemapper.classes.add(classNode);
                        }
                    } catch (Exception e) {
                        System.err.println("There was an error loading " + jarEntry.getName());
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    protected static void unloadJar() {
        if (!classes.isEmpty() || !files.isEmpty()) {
            classes.clear();
            files.clear();
        }
    }

    protected static byte[] toByteArray(ClassNode classNode) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        try {
            classNode.accept(writer);
            return writer.toByteArray();
        } catch (Throwable t) {
            writer = new ClassWriter(0);
            classNode.accept(writer);
            return writer.toByteArray();
        }
    }

    protected static void applyMappings(List <ClassNode> classMap, Map <String, String> remap) {
        SimpleRemapper remapper = new SimpleRemapper(remap);
        for (ClassNode node: new ArrayList < > (classMap)) {
            ClassNode copy = new ClassNode();
            ClassRemapper adapter = new ClassRemapper(copy, remapper);
            node.accept(adapter);
            classMap.remove(node);
            classMap.add(copy);
            node.sourceFile = null;
        }
    }

    protected static ClassNode getClassNode(String name, List <ClassNode> classMap) {
        return findFirst(classMap, c -> c.name.equals(name));
    }

    protected static <T> T findFirst(Collection < T > collection, Predicate < T > predicate) {
        for (T t: collection)
            if (predicate.test(t))
                return t;
        return null;
    }

    protected static boolean checkClassVerify(byte[] bytes) {
        return String.format("%X%X%X%X", bytes[0], bytes[1], bytes[2], bytes[3]).equals("CAFEBABE");
    }


    private static ArrayList <String> already = new ArrayList<>();
    private static ArrayList <String> chars = new ArrayList <String>();
    protected static String getRandomName() {
        if (chars.isEmpty()) {
        	String str = null;
        	
        	if (ClassRemapperAPI.customDictionary == null) {
            	str = ClassRemapperAPI.getDictionary();
        	}
        	else
        	{
        		str = ClassRemapperAPI.customDictionary;
        	}
        	
            for (char c: str.toCharArray()) {
                chars.add(String.valueOf(c));
            }
        }
        int characters = 128;
        String name = "";


        for (int i = 0; i < characters; i++) {
            String ch = chars.get(new Random().nextInt(chars.size()));
            name += ch;
        }
        return already.contains(name) ? getRandomName() : name;
    }
    
    protected static ClassNode getOwner(FieldNode f, List<ClassNode> classMap) {
        return findFirst(classMap, c -> c.fields.contains(f));
    }
}