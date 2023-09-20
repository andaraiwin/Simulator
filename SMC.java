import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class SMC {
    // SMC properties
    private static final int NUMMEMORY = 65536;
    private static final int NUMREGS = 8;
    private static final int MAXLINELENGTH = 1000;

    private int pc;
    private int[] mem;
    private int[] reg;
    private int numMemory;

    //Constructor
    public SMC() {
        pc = 0;
        mem = new int[NUMMEMORY];
        reg = new int[NUMREGS];
        numMemory = 0;
    }

    //Instruction simulation

    // Exit the program
    private void exit(int code) {
        System.out.println("Error: Exiting with code " + code);
        System.exit(code);
    }

    // Load the program
    public void loadProgram(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                mem[numMemory] = Integer.parseInt(line.trim());
                numMemory++;
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    // Run the program
    public void runProgram() {
        boolean isHalted = false;
        int numInstruction = 0;

        while (pc < numMemory && !isHalted) {
            if (numInstruction > 1000) break; // handle infinite loop

            numInstruction++; // Counting the number of executed instruction
            printState(); // print the previous state

            long instruction = mem[pc];

            // Check bits 25-31 equals to 0
            if (((instruction >> 25)) != 0) exit(1);

            // Check opcode 22-24 & return in Decimal
            short opcode;
            opcode = (short) ((instruction & 0x01C00000) >> 22);

            Field ext;
            ext = extractField(instruction);


            // Check Decimal of the opcode
            switch (opcode) {
                case 0 -> {
                    if ((instruction & 0x0000FFE0) != 0) exit(1);
                    reg[ext.rd] = reg[ext.regA] + reg[ext.regB];
                    pc++;
                }
                case 1 -> {
                    if ((instruction & 0x0000FFE0) != 0) exit(1);
                    reg[ext.rd] = ~(reg[ext.regA] & reg[ext.regB]);
                    pc++;
                }
                case 2 -> {
                    int address = reg[ext.regA] + ext.offsetField;
                    if (address < 0) exit(1);
                    reg[ext.regB] = mem[address];
                    pc++;
                }
                case 3 -> {
                    int memAddress = reg[ext.regA] + ext.offsetField;
                    if (memAddress < 0) exit(1);
                    mem[memAddress] = reg[ext.regB];
                    numMemory = Math.max(numMemory, memAddress + 1);
                    pc++;
                }
                case 4 -> {
                    int newPC = pc + 1 + ext.offsetField;
                    if (newPC < 0) exit(1);
                    if (reg[ext.regA] == reg[ext.regB]) pc = newPC;
                    else pc++;
                }
                case 5 -> {
                    if ((instruction & 0x0000FFFF) != 0) exit(1);
                    reg[ext.regB] = pc + 1;
                    pc = reg[ext.regA];
                }
                case 6 -> {
                    if ((instruction & 0x003FFFFF) != 0) exit(1);
                    isHalted = true;
                    pc++;
                }
                case 7 -> {
                    if ((instruction & 0x003FFFFF) != 0) exit(1);
                    pc++;
                }
                default -> System.out.println("error: opcode mismatch");
            }
        }

        System.out.println("machine halted");
        System.out.println("total of " + numInstruction + " instructions executed");
        System.out.println("final state of machine:");
        printState();
    }

    // Immediate field
    private int extendNumber(int num) {
        if ((num & (1 << 15)) != 0) {
            num -= (1 << 16);
        }
        return num;
    }

    // Eaxh address represents each line of code differently
    private Field extractField(long code) {
        Field extFields;
        extFields = new Field();
        extFields.regA = (int) ((code & 0x001C0000) >> 19);
        extFields.regB = (int) ((code & 0x00038000) >> 16);
        extFields.rd = (int) (code & 0x00000007);
        extFields.offsetField = extendNumber((int) (code & 0x0000FFFF));
        return extFields;
    }

    // Fields
    private static class Field {
        int regA;
        int regB;
        int rd;
        int offsetField;
    }

    // Print State
    private void printState () {
        System.out.println("\n@@@\nstate:\n");
        System.out.println("\tpc %d\n" + pc);
        System.out.println("\tmemory:\n");
        for (int i = 0; i < numMemory; i++) {
            System.out.println("\t\tmem[ " + i + " ] " + mem[i]);
        }
        System.out.println("\tregisters:\n");
        for (int i = 0; i < NUMREGS; i++) {
            System.out.println("\t\treg[ " + i + " ] " + reg[i]);
        }
        System.out.println("end state\n");
    }

    // Run program
    public static void Main (String[] args) {
        char[] line = new char[MAXLINELENGTH];

        if (args.length != 1) {
            System.out.println("error: usage: %s <machine-code file>\n");
            System.exit(1);
        }

        SMC processor = new SMC();

        try {
            processor.loadProgram(args[0]);
            processor.runProgram();
        } catch (IOException e) {
            System.out.println("error: can't open file " + args[0]);
            e.printStackTrace();
            System.exit(1);
        }


    }

}
