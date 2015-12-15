grammar InchwormAsm;   

/*======================================
 *    General Parser options
 *======================================*/

options { output=AST; } // Generate Abstract Syntax Tree


// Tokens used in AST
tokens {
  HEADER;
  OPTION;
  CONST;
  DATA;
  RAM;
  CODE;
  OP;
  REG;
  INSTRUCTION;
  NUM;  // Literal (non-negative) hex number
  SYMBOL; // Symbol: waiting for second pass.
  HERE;   // Current location (resolved in second pass)
  // Constant expression operations
  C_XOR;
	C_AND;
	C_OR;
	C_SHL;
	C_SHR;
	C_ADD;
	C_SUB;
	C_MUL;
	C_DIV;
	C_MOD;
	C_NOT;
	C_UNARY_MINUS;
} 

@lexer::header {
  package org.factcenter.inchworm.asm;

  import java.math.BigInteger;
}

@parser::header {
	package org.factcenter.inchworm.asm;

    import java.math.BigInteger;
	import java.util.HashMap;
	import org.factcenter.inchworm.*;
	import org.factcenter.inchworm.ops.*;
	import static org.factcenter.inchworm.Constants.*;
	import static org.factcenter.inchworm.Constants.NamedReg.*;
	import static org.factcenter.inchworm.ops.OpDefaults.*;
	import static org.factcenter.inchworm.VMState.*;
	import static org.factcenter.inchworm.asm.Assembler.*;

}

@parser::members {
  /**
   * Symbol Table
   */
	HashMap<String,BigInteger> symbols;
	
  	ArrayList<ExtraOpInfo> opInfo = new ArrayList<ExtraOpInfo>();
  	ArrayList<OpDesc> ops = new ArrayList<OpDesc>();
	
	int nextInstruction = 0; // Counter to keep track of instructions for label definitions

	VMState params;
	
	/**
	 * Initialize VM State. Call this before starting to parse.
	 */
	void initState(VMState params,  HashMap<String,BigInteger> symbols) {
	  this.symbols = symbols;
	  this.params = params;
	  nextInstruction = 0;
	  opInfo.clear();
	}
	
	VMState getVMState() {
	  return params;
	}
	
	
  @Override
  public Object recoverFromMismatchedSet(IntStream input, RecognitionException e, BitSet follow) 
      throws RecognitionException {
    throw e;
  }


  static class SyntaxError extends RecognitionException {
    String errorMessage;
    
    SyntaxError(String errorMessage, IntStream input) {
      super(input);
      this.errorMessage = String.format("\%s", errorMessage);
    }
  }
  
 /**
  * Override the displayRecognitionError to deal with custom errors.
  */
  @Override 
  public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
        if (e instanceof SyntaxError) {
          SyntaxError se = (SyntaxError) e;
          emitErrorMessage(se.errorMessage);
        } else {
          super.displayRecognitionError(tokenNames, e);
        }
    }
}

/*======================================
 *    Lexer grammar (defines tokens)
 *======================================*/

PERCENT
  : '%'
  ;

STAR
  : '*'
  ;
  
 
// A register prefix token is %r, but only if followed by digits or '['
// We need to use a semantic predicate to check this so that identifiers starting with the
// letter r won't be confused. 
REG_PREFIX  
  : { (input.LA(3) >= '0') && (input.LA(3) <= '9') || (input.LA(3) =='[') }?=> PERCENT 'r'  
  ;

CURRENT_POS // Current instruction or register.
  : '$'
  ;

STRING
@init { final StringBuilder buf = new StringBuilder(); }
    :
    '"'  (  ESCAPE[buf] | i = ~( '\\' | '"' ) { buf.appendCodePoint(i); }  )*  '"'
      { setText(buf.toString()); }
    ;

fragment ESCAPE[StringBuilder buf]
    : '\\' ( 
      't' { buf.append('\t'); }
    | 'n' { buf.append('\n'); }
    | 'r' { buf.append('\r'); }
    | '"' { buf.append('\"'); }
    | '\\' { buf.append('\\'); }
    )
    ;

COMMENT
    :   ('//'|';') ~('\n'|'\r')* '\r'? '\n' {$channel=HIDDEN;}
    |   '/*' (options {greedy=false;} : .)* '*/' {$channel=HIDDEN;}
    ;

WS  :   ( ' ' | '\t' | '\r' | '\n' ) {$channel=HIDDEN;}   ;
    
 
EOI	:	'---'	// End-of-instruction
	;

INSTRUCTIONDEF
	:	'instruction'
	;


// Register names 
NOP	:	'nop'
	;

CARRY
	:	'carry'
	;
SIGN
	:	'sign'
	;
OVERFLOW
	:	'overflow'
	;
FLAGS
	:	'flags'
	;
IP	:	'ip'
	;

OUT1:	'out1'
	;
	
OUT2:	'out2'
	;
	
CTRL: 'ctrl'
  ;
  
// Counter flag prefix
// (Counter flags are special registers of the form ctrX, where X must be a modulus declared in the header section)
CTR_PREFIX
  :  'ctr'  
  ;

CTR_FLAG
  : CTR_PREFIX DEC_DIGITS {setText(Long.toHexString(Long.parseLong($DEC_DIGITS.getText(), 10)));}
  ; 
  
// Local regs prefix
// Local regs are special registers of the form %localX (or %local[X]), where X must be 
// from 0 till the frame size declared in the header section.
LOCAL_PREFIX
  : 'local'  
  ;
  
LOCAL_REG
  : LOCAL_PREFIX DEC_DIGITS {setText(Long.toHexString(Long.parseLong($DEC_DIGITS.getText(), 10)));}
  | LOCAL_PREFIX  '[' DEC_DIGITS ']' {setText(Long.toHexString(Long.parseLong($DEC_DIGITS.getText(), 10)));}
  ;

FREEREGS    // The number of 'free' (general purpose) registers; also the index of the first special register.
  : 'freeregs'
  ;

// ZERO (also used as an op, so defined there)
// HALT (also used as an op, so defined there)
// IN	(also used as an op, so defined there)

// Opcodes
ZERO	: 'zero' ;	// Zero a register
XORI	: 'xori' ;	// Xor a register with an immediate
ADD		: 'add';	// Add two registers and store in a third
SUB 	: 'sub' ;	// Subtract two registers one from another and store in a third
ROL		: 'rol';	// Rotate-left a register by a number of bits stored in another register and store in a third.
AND   	: 'and';    // Bitwise and two registers and store in a third - added 18 sep, 2012
XOR		: 'xor';
OR   	: 'or';
MUL		: 'mul';
DIV		: 'div';
LOADREG : 'loadreg';    // Load indirect from registers
STOREREG: 'storereg';   // Store indirect to registers
LOAD	: 'load';		// Load indirect from RAM
STORE	: 'store';	// Store indirect to RAM
OUT		: 'out';	// Output to parties
IN		: 'in';		// Input from parties
HALT  	: 'halt';
MUX		: 'mux';
NEXT	: 'next';
CALL 	: 'call';
RETURN	: 'return';


// Header definitions
WORDSIZE:	'wordsize';	// word size (registers have this length in bits [default is 32]
RAMWORDSIZE
    : 'ramwordsize'; // size of RAM word [default is same as wordsize]
REGPTRSIZE
	:	'regptrsize';	// log of number of registers (there are 2^regptrsize registers)
ROMPTRSIZE
	:	'romptrsize';	// log of number of instructions
RAMPTRSIZE
	:	'ramptrsize';	// log of number of RAM words (size of a ram pointer).
RAMLOADSIZE
	:   'ramloadsize';  // number of RAM words that are loaded/stored in a single load/store instruction
STACKPTRSIZE
  : 'stackptrsize'; // log of number of stack items
FRAMESIZE
  : 'framesize';    // number of frame registers
  
COUNTERS
  : 'counters'; // Declaration of counter flag registers. The header should have a space (or comma) separated list of
                // of modulus values -- each modulus defines a corresponding counter flag register.

// Operations for constant expressions
c_xor	:	'^';
c_and	:	'&';
c_or	:	'|';
c_shl	:	'<<';
c_shr	:	'>>';
c_add	:	'+';
c_sub	:	'-';
c_mul	:	STAR;
c_div	:	'/';
c_mod	:	PERCENT;
c_not	:	'~';


HEX_LITERAL
	:	'0x' HEX_DIGITS  { setText($HEX_DIGITS.text); }
	;

BINARY_LITERAL
	:	'0b' BIN_DIGITS { setText((new BigInteger($BIN_DIGITS.text, 2)).toString(16)); }
	;
	
DECIMAL_LITERAL
	:	DEC_DIGITS { setText((new BigInteger($DEC_DIGITS.text, 10)).toString(16)); }
	;
	
	
fragment BIN_DIGITS
	:	('0'|'1')+  
	;
	
fragment DEC_DIGITS
	:	'0'..'9'+  
	;

fragment HEX_DIGITS
	:	('0'..'9'|'a'..'f'|'A'..'F')+
	;


/**
 Section Names
 **/
SEC_HEADER
	:	'.header'
	;
	
SEC_DATA
	:	'.data'
	;

SEC_RAM
	:	'.ram'
	;
	
SEC_CODE
	:	'.code'
	;

SEC_CONST
	:	'.const'
	;

SEC_END
  : '.end'
  | 'end'
  ;

ID  : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*
    ;

/*======================================
 *    Parser grammar
 *======================================*/ 
 
  
program	:	header_section? const_section? data_section? code_section? ram_section? SEC_END?
	;

header_section
	:	(SEC_HEADER?) opts=header_option* ins=instruction_def -> ^(HEADER $opts $ins)
	;
	
// Instruction definition
instruction_def
	:	INSTRUCTIONDEF (':'?) opdesc+ { params.setOpDescs(ops); } -> ^(INSTRUCTIONDEF opdesc+)
	;
	
opdesc
  :	STAR? opname ':' freq 
     {ops.add(new OpDesc(getDefaultDesc(Op.getEnum($opname.text)), Integer.parseInt($freq.text, 16)));
      opInfo.add(new ExtraOpInfo($STAR != null));} -> ^(OP opname freq STAR?)
	|	STAR? opname 
	   {ops.add(getDefaultDesc(Op.getEnum($opname.text)));
	    opInfo.add(new ExtraOpInfo($STAR != null));} -> ^(OP opname NUM["1"] STAR?)
	;

opname 	:	ZERO | XORI | XOR | OR | ADD | SUB | ROL | AND | MUL | DIV | LOAD | STORE | LOADREG |
          STOREREG | OUT | IN | HALT | MUX | CALL | RETURN | NEXT
	;

freq	:	num 
	; // frequency (execute every freq times)
	
header_option
	:	WORDSIZE ':'? num {params.setWordSize(Integer.parseInt($num.text, 16));}	-> ^(OPTION WORDSIZE num)
	|	RAMWORDSIZE ':'? num {params.setRamWordSize(Integer.parseInt($num.text, 16));}	-> ^(OPTION RAMWORDSIZE num)
	|	REGPTRSIZE ':'? num {params.setRegPtrSize(Integer.parseInt($num.text, 16));}	-> ^(OPTION REGPTRSIZE num)
	|	ROMPTRSIZE ':'? num	{params.setRomPtrSize(Integer.parseInt($num.text, 16));} -> ^(OPTION ROMPTRSIZE num)
	|	RAMPTRSIZE ':'? num	{params.setRamPtrSize(Integer.parseInt($num.text, 16));} -> ^(OPTION RAMPTRSIZE num)
	|	RAMLOADSIZE ':'? num {
	        params.setRamLoadSize(Integer.parseInt($num.text, 16));
	        if (Integer.bitCount(params.getRamLoadSize()) != 1)
	            throw new SyntaxError("Ram loadsize must be a power of 2!", this.input);
	        if (params.getRamSize() \% params.getRamLoadSize() != 0)
                throw new SyntaxError("Ram loadsize must divide ram size!", this.input);
	    } -> ^(OPTION RAMLOADSIZE num)
	| STACKPTRSIZE ':'? num {params.setStackPtrSize(Integer.parseInt($num.text, 16));} -> ^(OPTION STACKPTRSIZE num)
	| FRAMESIZE ':'? num {params.setFrameSize(Integer.parseInt($num.text, 16));} -> ^(OPTION FRAMESIZE num)
	| COUNTERS ':'? countermodulus+ -> ^(OPTION COUNTERS countermodulus+) 
	;
	
countermodulus
  : num (','?) {params.addCounter(Integer.parseInt($num.text, 16));}
  ;	
	
const_section
	: 	SEC_CONST const_def*	-> ^(CONST const_def*)
	;

const_def
  : reserved_label ('='|'db')! const_expr { 
      if (true) 
        throw new SyntaxError("Attempt to redefine reserved label: " + $reserved_label.text, this.input); 
    }
	|	ID ('='|'db')! const_expr -> ^(ID const_expr)
	;

data_section
  : SEC_DATA data_def* -> ^(DATA data_def*) 
  ;
  
  
data_def
  : reserved_label ':' { 
      if (true) 
        throw new SyntaxError("Attempt to redefine reserved label: " + $reserved_label.text, this.input); 
    }
  | ID ':'                  -> ID     // Data label (IDs are resolved in pass2)
  | current_pos_ref                   // Syntactic sugar: allow "label: $" to mean the previous location.
  | reg  (':' | '=' | 'db') -> reg    // location indicator. Specifies the register location for next data assignment
  | STRING (','?)           -> STRING // Assignment of a string to a sequence of registers (increments current register by length of string).
  | '-' num (','?)          -> ^(C_UNARY_MINUS num) // Allow negative numbers without parenthesis
  | num (','?)              -> num  // Assignment to current register, increments current register
  | '(' const_expr ')' (','?)  -> const_expr  // Assignment to current register, increments current register 
  ;  

ram_section
  : SEC_RAM ram_def* -> ^(RAM ram_def*) 
  ;
  
  
ram_def
  : reserved_label ':' { 
      if (true) 
        throw new SyntaxError("Attempt to redefine reserved label: " + $reserved_label.text, this.input); 
    }
  | ID ':'                  -> ID     // Data label (IDs are resolved in pass2)
 // | STRING (','?)           -> STRING // Assignment of a string to a sequence of memory cells (increments current memory cell by length of string).
//  | '-' num (','?)          -> ^(C_UNARY_MINUS num) // Allow negative numbers without parenthesis
  | num (','?)              -> num  // Assignment to current memory cell, increments current memory cell
//  | '(' const_expr ')' (','?)  -> const_expr  // Assignment to current memory cell, increments current memory cell 
  ;

code_section	
	:	SEC_CODE codeop* -> ^(CODE codeop*)
	;

codeop	:	label_def | instruction
	;

immediate
	:	const_expr
	;

const_expr
	:	const_prec5
	;

const_prec5
  : l=const_prec4 (const_prec5_op^ r=const_prec4)* //-> ^(const_prec5_op $l $r)
//  | const_prec4 
  ;
	
const_prec4
  : l=const_prec3 (const_prec4_op^ r=const_prec3)* //-> ^(const_prec4_op $l $r)
//  | const_prec3 
  ;

const_prec3
  : l=const_prec2 (const_prec3_op^ r=const_prec2)* //-> ^(const_prec3_op $l $r)
//  | const_prec2 
  ;

const_prec2
  : l=const_prec1 (const_prec2_op^ r=const_prec1)* //-> ^(const_prec2_op $l $r)
//  | const_prec1 
  ;

const_prec1
  : l=const_prec0 (const_prec1_op^ r=const_prec0)* //-> ^(const_prec1_op $l $r)
//  | const_prec0 
  ;

const_prec0
  : const_unary_op^ const_atom  //-> ^(const_unary_op const_atom)
  | const_atom
  ;
 
const_atom
  : num | symbol_ref | '(' const_expr ')' -> const_expr 
  ;
  
const_unary_op
  : c_not  -> C_NOT
  | c_sub  -> C_UNARY_MINUS
  ;

const_prec1_op // first precedence level
  : c_mul  -> C_MUL
  | c_div  -> C_DIV
  | c_mod  -> C_MOD
  ;
  
const_prec2_op // second precedence level
  : c_add  -> C_ADD
  | c_sub  -> C_SUB
  ;

const_prec3_op
  : c_shl  -> C_SHL
  | c_shr  -> C_SHR
  ;

const_prec4_op
  : c_and  -> C_AND
  | c_xor  -> C_XOR // technically, this should be precedence 4 1/2
  ;
  
const_prec5_op
  : c_or  -> C_OR
  ;

// Reserved labels
reserved_label
  : reg_symbol
  ;

label_def
  : reserved_label ':' { 
      if (true) 
        throw new SyntaxError("Attempt to redefine reserved label: " + $reserved_label.text, this.input); 
    }
	|	ID! ':'! { symbols.put($ID.text, BigInteger.valueOf(nextInstruction)); }
	;

symbol_ref
	:	ID -> ^(SYMBOL ID)
	| current_pos_ref
	|	reg_symbol // We allow register names to be used as symbols, since their location may depend on size of register file.
	;
	
current_pos_ref
	: CURRENT_POS      -> HERE
	;

instruction 
	:	op+ (EOI|EOF) { ++nextInstruction; } -> ^(INSTRUCTION NUM[Integer.toHexString(nextInstruction - 1)]  op+) 
	;

/****
 List of operations
 ****/	
 	
// Note: if you change number of arguments, also change the corresponding definition in Constants.java.
op:	ZERO^ arg1 
	|	XORI^ arg1i
	| XOR^ arg3
	| OR^ arg3
	| ADD^ arg3
	| SUB^ arg3
	| AND^ arg3
	|	ROL^ arg3
	|	MUL^ arg3 | DIV^ arg3 | LOAD^ arg2 | STORE^ arg2 | LOADREG^ arg2 | STOREREG^ arg2
	| OUT | IN | HALT | MUX^ arg4 | NEXT^ arg2 | CALL^ argi | RETURN
	;

/** 
 Arguments to operations
 **/	
argi : immediate
  ;

arg1	:	reg
	|	NOP		-> ^(REG NOP)	// Don't have to use register notation for nop
	;
	
arg1i	:	reg (','|'<')! immediate
	|	NOP		-> ^(REG NOP) ^(NUM["0"])	
	;	

arg2	:	reg (','|'<')! reg
	|	NOP		-> ^(REG NOP) ^(REG NOP)	
	;
	
arg3	:	reg (','|'<')! reg ','! reg
	|	NOP		-> ^(REG NOP) ^(REG NOP) ^(REG NOP)	
	;
	
arg4	:	reg (','|'<')! reg ','! reg ','! reg
	|	NOP		-> ^(REG NOP) ^(REG NOP) ^(REG NOP) ^(REG NOP)
	;
	
reg	:	REG_PREFIX num				-> ^(REG num)
	|	REG_PREFIX '[' const_expr ']'		-> ^(REG const_expr)
	|	PERCENT symbol_ref		-> ^(REG symbol_ref)
	;
	
reg_symbol
	: IP 		-> NUM[Integer.toHexString(params.getNumRegs() - R_IP.getOffset())]
	| OUT1 		-> NUM[Integer.toHexString(params.getNumRegs() - R_OUT1.getOffset())]
	| OUT2 		-> NUM[Integer.toHexString(params.getNumRegs() - R_OUT2.getOffset())]
	| IN 		-> NUM[Integer.toHexString(params.getNumRegs() - R_IN.getOffset())]
  	| CTRL   -> NUM[Integer.toHexString(params.getNumRegs() - R_CTRL.getOffset())]
	| CARRY 	-> NUM[Integer.toHexString(params.getNumRegs() - R_CARRY.getOffset())]
	| ZERO 		-> NUM[Integer.toHexString(params.getNumRegs() - R_ZERO.getOffset())]
	| SIGN 		-> NUM[Integer.toHexString(params.getNumRegs() - R_SIGN.getOffset())]
	| OVERFLOW	-> NUM[Integer.toHexString(params.getNumRegs() - R_OVERFLOW.getOffset())]
	| FLAGS 	-> NUM[Integer.toHexString(params.getNumRegs() - R_FLAGS.getOffset())]
	| NOP		-> NUM[Integer.toHexString(params.getNumRegs() - R_NOP.getOffset())]
	| CTR_FLAG {      
	    int modulus = Integer.parseInt($CTR_FLAG.text, 16);
      int regIndex = params.getCounterRegisterLocation(modulus);
      if (regIndex < 0)
        throw new SyntaxError("ctr"+modulus+" was used but not declared in header: ", this.input);
    } -> NUM[Integer.toHexString(regIndex)]
  | LOCAL_REG {
        int localNdx = Integer.parseInt($LOCAL_REG.text, 16);
        int regIndex = params.getLocalRegisterLocation(localNdx);
        if (regIndex < 0)
        throw new SyntaxError("local"+localNdx+" is out of the defined range. ", this.input);
  } -> NUM[Integer.toHexString(regIndex)]
	| FREEREGS -> NUM[Integer.toHexString(params.getNumRegs() - params.getFirstSpecialOffset())]
	;	

num	:	(HEX_LITERAL | BINARY_LITERAL | DECIMAL_LITERAL) -> NUM[$text]
	;

