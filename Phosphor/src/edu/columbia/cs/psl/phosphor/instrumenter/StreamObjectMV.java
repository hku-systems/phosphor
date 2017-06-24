package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Created by jianyu on 6/24/17.
 */
public class StreamObjectMV extends MethodVisitor {

    static private Class<?> taintedArraySupperClass =
            (Configuration.MULTI_TAINTING) ? LazyArrayObjTags.class : LazyArrayIntTags.class;

    String methodName;

    String className;

    StreamObjectMV(MethodVisitor mv, String cn, String mn) {
        super(Opcodes.ASM5, mv);
        methodName = mn;
        className = cn;
    }

    @Override
    public void visitMethodInsn(int opcodes, String cn, String mn, String desc, boolean b) {
        if (methodName.equals("set")
                && (className.equals("sun/reflect/UnsafeObjectFieldAccessorImpl")
                    || className.equals("sun/reflect/UnsafeStaticObjectFieldAccessorImpl"))) {
            // make assiable true
            // invoke the unwrap method

            // this method may be instrumented?? it should not
            System.out.println("find set");
            if (cn.equals("sun/misc/Unsafe") && mn.startsWith("putObject")) {
                System.out.println("find put");
                super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(StreamObjectMV.class), mn, "(Lsun/misc/Unsafe;"+desc.substring(1), b);
                return;
            }
        } else if (methodName.equals("setObjFieldValues")
                && className.equals("java/io/ObjectStreamClass$FieldReflector")) {
            if (cn.equals("sun/misc/Unsafe") && mn.startsWith("putObject")) {
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitFieldInsn(Opcodes.GETFIELD, "java/io/ObjectStreamClass$FieldReflector", "tmp_class$$PHOSPHOR", "Ljava/lang/Class;");
                if(!Configuration.MULTI_TAINTING)
                  super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(StreamObjectMV.class), mn,
                        "(Lsun/misc/Unsafe;Ljava/lang/Object;IJLjava/lang/Object;Ljava/lang/Class;)V", b);
                else
                  super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(StreamObjectMV.class), mn,
                        "(Lsun/misc/Unsafe;Ljava/lang/Object;Ledu/columbia/cs/psl/phosphor/runtime/Taint;JLjava/lang/Object;Ljava/lang/Class;)V", b);

                return;
            } else if (mn.startsWith("isInstance")) {
                System.out.println("instance");
                super.visitInsn(Opcodes.POP2);
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitInsn(Opcodes.SWAP);
                super.visitFieldInsn(Opcodes.PUTFIELD, "java/io/ObjectStreamClass$FieldReflector", "tmp_class$$PHOSPHOR", "Ljava/lang/Class;");
//                super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(StreamObjectMV.class),
//                        "emitTrue", "()Ledu/columbia/cs/psl/phosphor/struct/TaintedBooleanWithIntTag;", false);
                super.visitTypeInsn(Opcodes.NEW,
                  Configuration.MULTI_TAINTING ? Type.getInternalName(TaintedBooleanWithIntTag.class)
                    : Type.getInternalName(TaintedBooleanWithObjTag.class));
                super.visitInsn(Opcodes.DUP);
                if (!Configuration.MULTI_TAINTING)
                  super.visitInsn(Opcodes.ICONST_0);
                else
                  super.visitInsn(Opcodes.ACONST_NULL);
                super.visitInsn(Opcodes.ICONST_1);
                if (!Configuration.MULTI_TAINTING)
                  super.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(TaintedBooleanWithIntTag.class), "<init>", "(IZ)V", false);
                else
                  super.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(TaintedBooleanWithObjTag.class),
                    "<init>", "(Ledu/columbia/cs/psl/phosphor/runtime/Taint;Z)V", false);
                return;
            }
        }

        if (cn.equals("java/io/ObjectInputStream") && mn.startsWith("readOrdinaryObject")) {
            System.out.println("input");
            super.visitMethodInsn(opcodes, cn, mn, desc, b);
            super.visitInsn(Opcodes.DUP);
            super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(StreamObjectMV.class), "resolveObject",
                    "(Ljava/lang/Object;)V", b);
            return;
        }

        super.visitMethodInsn(opcodes, cn, mn, desc, b);
    }

    static public void putObject$$PHOSPHORTAGGED(Unsafe unsafe, Object obj, int tag, long key, Object val, Class c) {
        if (val != null && !c.isAssignableFrom(val.getClass())) {
            val = ((LazyArrayIntTags)val).getVal();
        }
        unsafe.putObject(obj, key, val);
    }

    static public void putObject$$PHOSPHORTAGGED(Unsafe unsafe, Object obj, Taint tag, long key, Object val, Class c) {
        if (val != null && !c.isAssignableFrom(val.getClass())) {
            val = ((LazyArrayObjTags)val).getVal();
        }
        unsafe.putObject(obj, key, val);
    }

    static public void putObject$$PHOSPHORTAGGED(Unsafe unsafe, Object obj, int tag, long key, Object val) {
        unsafe.putObject(obj, key, val);
    }

    static public void putObject$$PHOSPHORTAGGED(Unsafe unsafe, Object obj, Taint tag, long key, Object val) {
      unsafe.putObject(obj, key, val);
    }
    static public void resolveObject(Object obj) {
        if (obj == null)
            return;

        Class c = obj.getClass();
        Field[] fs = c.getFields();
        for (Field f:
                fs) {
			/* we only need to modify the tag for array */
            String field_name = f.getName();
            if (field_name.endsWith("PHOSPHOR_TAG") &&
                    taintedArraySupperClass.isAssignableFrom(f.getType())) {
                String original_name = f.getName().substring(0, field_name.length() - 12);
                try {
                    f.setAccessible(true);
                    Field of = c.getDeclaredField(original_name);
                    of.setAccessible(true);
                    Object ob = of.get(obj);
                    if (ob == null) continue;
                    if (f.get(obj) != null) continue;
                    if (taintedArraySupperClass.isAssignableFrom(ob.getClass())) {
                        f.set(obj, ob);
                    } else if (taintedArraySupperClass == LazyArrayIntTags.class) {
                        Class tc = ob.getClass();
                        Object to = null;
                        if (int[].class.isAssignableFrom(tc)) {
                            to = new LazyIntArrayIntTags((int[])ob);
                        } else if (short[].class.isAssignableFrom(tc)) {
                            to = new LazyShortArrayIntTags((short[])ob);
                        } else if (long[].class.isAssignableFrom(tc)) {
                            to = new LazyLongArrayIntTags((long[])ob);
                        } else if (double[].class.isAssignableFrom(tc)) {
                            to = new LazyDoubleArrayIntTags((double[])ob);
                        } else if (float[].class.isAssignableFrom(tc)) {
                            to = new LazyFloatArrayIntTags((float[])ob);
                        } else if (char[].class.isAssignableFrom(tc)) {
                            to = new LazyCharArrayIntTags((char[])ob);
                        } else if (byte[].class.isAssignableFrom(tc)) {
                            to = new LazyByteArrayIntTags((byte[])ob);
                        } else if (boolean[].class.isAssignableFrom(tc)) {
                            to = new LazyBooleanArrayIntTags((boolean[])ob);
                        } else {
                            throw new IllegalAccessException("wrong type");
                        }
                        f.set(obj, to);
                    } else if (taintedArraySupperClass == LazyArrayObjTags.class) {
                        Class tc = ob.getClass();
                        Object to = null;
                        if (int[].class.isAssignableFrom(tc)) {
                            to = new LazyIntArrayObjTags((int[])ob);
                        } else if (short[].class.isAssignableFrom(tc)) {
                            to = new LazyShortArrayObjTags((short[])ob);
                        } else if (long[].class.isAssignableFrom(tc)) {
                            to = new LazyLongArrayObjTags((long[])ob);
                        } else if (double[].class.isAssignableFrom(tc)) {
                            to = new LazyDoubleArrayObjTags((double[])ob);
                        } else if (float[].class.isAssignableFrom(tc)) {
                            to = new LazyFloatArrayObjTags((float[])ob);
                        } else if (char[].class.isAssignableFrom(tc)) {
                            to = new LazyCharArrayObjTags((char[])ob);
                        } else if (byte[].class.isAssignableFrom(tc)) {
                            to = new LazyByteArrayObjTags((byte[])ob);
                        } else if (boolean[].class.isAssignableFrom(tc)) {
                            to = new LazyBooleanArrayObjTags((boolean[])ob);
                        } else {
                            throw new IllegalAccessException("wrong type");
                        }
                        f.set(obj, to);
                    } else {
                        throw new IllegalAccessException("error array taint type");
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
