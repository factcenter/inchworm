package org.factcenter.inchworm.ops.concrete;

import org.factcenter.inchworm.VMOpImplementation;
import org.factcenter.inchworm.VMRunner;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.inchworm.ops.OpDefaults;
import org.factcenter.inchworm.ops.VMProtocolParty;
import org.factcenter.inchworm.ops.common.*;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;

import java.io.IOException;
import java.util.*;

/**
 * Creates the set of concrete ops for the Inchworm Virtual Machine.
 */
public class ConcreteOPFactory extends ConcreteCommon implements VMOpImplementation {

	/*-
	 * ----------------------------------------------------------------
	 *                  ConcreteOPFactory Ops members.
	 * ----------------------------------------------------------------
	 */
//	AddOp addOp;
//	AndOp andOp;
//	DivOp divOp;
//	HaltOp haltOp;
//	FastMux loadOp;
//	MulOp mulOp;
//	MuxOp muxOp;
//	NextOp nextOp;
//	OrOp orOp;
//	OutOp outOp;
//	RolOp rolOp;
//	FastUnMux storeOp;
//	XorOp xorOp;
//	XoriOp xoriOp;
//	ZeroOp zeroOp;
//	CallOp callOp;
//	RetOp retOp;
//	LoadStoreMem loadMemOp;

	Map<String, OpAction> ops;
    List<VMProtocolParty> opSetup;



	/*-
	 * ----------------------------------------------------------------
	 *                  ConcreteOPFactory OT members.
	 * ----------------------------------------------------------------
	 */

	/*-
	 * ----------------------------------------------------------------
	 *                  ConcreteOPFactory CTor.
	 * ----------------------------------------------------------------
	 */
	public ConcreteOPFactory() {
        super(null);
        ops = new HashMap<>();
        opSetup = new ArrayList<>();

        Mov mov = new Mov(this);
        ops.put(OpDefaults.Op.OP_LOAD.name, mov);
        ops.put(OpDefaults.Op.OP_STORE.name, mov);
        ops.put(OpDefaults.Op.OP_LOADREG.name, mov);
        ops.put(OpDefaults.Op.OP_STOREREG.name, mov);
        ops.put(OpDefaults.Op.OP_ZERO.name, mov);

        Xor xor = new Xor(this);
        ops.put(OpDefaults.Op.OP_XORI.name, xor);
        ops.put(OpDefaults.Op.OP_XOR.name, xor);

        AddOp add = new AddOp();
        opSetup.add(add);
        ops.put(OpDefaults.Op.OP_ADD.name, add);
        ops.put(OpDefaults.Op.OP_NEXT.name, add);

        SubOp sub = new SubOp();
        opSetup.add(sub);
        ops.put(OpDefaults.Op.OP_SUB.name, sub);
        
        AndOp and = new AndOp();
        opSetup.add(and);
        ops.put(OpDefaults.Op.OP_AND.name, and);

        OrOp or = new OrOp();
        opSetup.add(or);
        ops.put(OpDefaults.Op.OP_OR.name, or);

        RolOp rol = new RolOp();
        opSetup.add(rol);
        ops.put(OpDefaults.Op.OP_ROL.name, rol);

        MuxOp mux = new MuxOp();
        opSetup.add(mux);
        ops.put(OpDefaults.Op.OP_MUX.name, mux);

        MulOp mul = new MulOp();
        opSetup.add(mul);
        ops.put(OpDefaults.Op.OP_MUL.name, mul);

        DivOp div = new DivOp();
        opSetup.add(div);
        ops.put(OpDefaults.Op.OP_DIV.name, div);

        Halt halt = new Halt(this);
        ops.put(OpDefaults.Op.OP_HALT.name, halt);

        CallOp call = new CallOp();
        opSetup.add(call);
        ops.put(OpDefaults.Op.OP_CALL.name, call);

        RetOp ret = new RetOp();
        opSetup.add(ret);
        ops.put(OpDefaults.Op.OP_RETURN.name, ret);

        Out out = new Out(this);
        ops.put(OpDefaults.Op.OP_OUT.name, out);

        In in = new In(this);
        ops.put(OpDefaults.Op.OP_IN.name, in);
	}

	/*-
	 * ----------------------------------------------------------------
	 *                VMProtocolParty Overrides.
	 * ----------------------------------------------------------------
	 */
	@Override
	public void setParameters(int playerNum, VMState state, VMRunner runner, Random rand) {
        super.setParameters(playerNum, state, runner, rand);
		// OT parameters.
		Channel toPeer = runner.getChannel();
		otExtender.setParameters(toPeer, rand);

		for (VMProtocolParty op : opSetup) {
			op.setParameters(playerNum, state, runner, rand);
		}

	}

	@Override
	public void setMoreParameters(OTExtender otExtender) {
		super.setMoreParameters(otExtender);
		for (VMProtocolParty op : opSetup) {
			if (op instanceof ConcreteCommon) {
				((ConcreteCommon) op).setMoreParameters(otExtender);
			}
		}

	}

	@Override
	public void init() throws IOException, InterruptedException {
		for (VMProtocolParty op : opSetup) {
			op.init();
		}
	}



    @Override
    public OpAction getOpAction(String opName) {
        return ops.get(opName);
    }
}
