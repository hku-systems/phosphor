package edu.columbia.cs.psl.phosphor.instrumenter;


import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.struct.LazyLongArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyLongArrayObjTags;
import jdk.nashorn.internal.runtime.regexp.joni.constants.OPCode;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static edu.columbia.cs.psl.phosphor.TaintUtils.TAINT_FIELD;

/**
 * Created by jianyu on 6/21/17.
 */
public class NativeNullMV extends MethodVisitor {
    NativeNullMV(MethodVisitor mv) {
        super(Opcodes.ASM5, mv);
    }

    private static String UNIX_SYSTEM_CLASS = "com/sun/security/auth/module/UnixSystem";

    private static Class LazyClass = Configuration.MULTI_TAINTING ? LazyLongArrayObjTags.class: LazyLongArrayIntTags.class;

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean ift) {
        super.visitMethodInsn(opcode, owner, name, desc, ift);
        if (owner.equals(UNIX_SYSTEM_CLASS) && name.equals("getUnixInfo")) {
            super.visitVarInsn(Opcodes.ALOAD, 0);
            super.visitFieldInsn(Opcodes.GETFIELD, UNIX_SYSTEM_CLASS,"groups", "[J");
            super.visitTypeInsn(Opcodes.NEW,
                    Type.getInternalName(LazyClass));
            super.visitInsn(Opcodes.DUP_X1);
            super.visitInsn(Opcodes.SWAP);
            super.visitMethodInsn(Opcodes.INVOKESPECIAL,
                    Configuration.MULTI_TAINTING ? "edu/columbia/cs/psl/phosphor/struct/LazyLongArrayObjTags":
                            "edu/columbia/cs/psl/phosphor/struct/LazyLongArrayIntTags",
                    "<init>", "([J)V", false);
            super.visitVarInsn(Opcodes.ALOAD, 0);
            super.visitInsn(Opcodes.SWAP);
            super.visitFieldInsn(Opcodes.PUTFIELD, UNIX_SYSTEM_CLASS, "groups" + TAINT_FIELD, Configuration.MULTI_TAINTING ? "Ledu/columbia/cs/psl/phosphor/struct/LazyLongArrayObjTags;":
                    "Ledu/columbia/cs/psl/phosphor/struct/LazyLongArrayIntTags;");
        }
    }
}
