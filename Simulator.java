import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Simulator {
    //SMC Computer
    private static final int NUMMEMORY = 65536;
    private static final int NUMREGS = 8;
    private static final int MAXLINELENGTH = 1000;

    private int pc;
    private int[] mem;
    private int[] reg;
    private int numMemory;

    // Initializer
    public Simulator() {
        mem = new int[NUMMEMORY];
        reg = new int[NUMREGS];
        numMemory = 0;
        pc = 0;
    }


    public void loadProgram(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                mem[numMemory] = Integer.parseInt(line.trim());
                numMemory++;
            }
        }
    }

    public void runProgram() {
        boolean isHalted = false;
        int numInstruction = 0;

        while (pc < numMemory && !isHalted) {
            if (numInstruction > 1000) break;

            numInstruction++;
            printState();

            long instruction = mem[pc];
            if (((instruction >> 25) | 0) != 0) exit(1);

            short opcode = (short) ((instruction & 0x01C00000) >> 22);
            Field ext = extractField(instruction);

            switch (opcode) {
                case 0:
                    if ((instruction & 0x0000FFE0) != 0) exit(1);
                    reg[ext.rd] = reg[ext.regA] + reg[ext.regB];
                    pc++;
                    break;
                case 1:
                    if ((instruction & 0x0000FFE0) != 0) exit(1);
                    reg[ext.rd] = ~(reg[ext.regA] & reg[ext.regB]);
                    pc++;
                    break;
                case 2:
                    int address = reg[ext.regA] + ext.offsetField;
                    if (address < 0) exit(1);
                    reg[ext.regB] = mem[address];
                    pc++;
                    break;
                case 3:
                    int memAddress = reg[ext.regA] + ext.offsetField;
                    if (memAddress < 0) exit(1);
                    mem[memAddress] = reg[ext.regB];
                    numMemory = Math.max(numMemory, memAddress + 1);
                    pc++;
                    break;
                case 4:
                    int newPC = pc + 1 + ext.offsetField;
                    if (newPC < 0) exit(1);
                    if (reg[ext.regA] == reg[ext.regB]) pc = newPC;
                    else pc++;
                    break;
                case 5:
                    if ((instruction & 0x0000FFFF) != 0) exit(1);
                    reg[ext.regB] = pc + 1;
                    pc = reg[ext.regA];
                    break;
                case 6:
                    if ((instruction & 0x003FFFFF) != 0) exit(1);
                    isHalted = true;
                    pc++;
                    break;
                case 7:
                    if ((instruction & 0x003FFFFF) != 0) exit(1);
                    pc++;
                    break;
                default:
                    System.out.println("error: opcode mismatch");
            }
        }

        System.out.println("machine halted");
        System.out.println("total of " + numInstruction + " instructions executed");
        System.out.println("final state of machine:");
        printState();
    }

    private int extendNumber(int num) {
        if ((num & (1 << 15)) != 0) {
            num -= (1 << 16);
        }
        return num;
    }

    private Field extractField(long code) {
        Field extFields = new Field();
        extFields.regA = (int) ((code & 0x001C0000) >> 19);
        extFields.regB = (int) ((code & 0x00038000) >> 16);
        extFields.rd = (int) (code & 0x00000007);
        extFields.offsetField = extendNumber((int) (code & 0x0000FFFF));
        return extFields;
    }

    private void printState() {
        System.out.println("\n@@@\nstate:");
        System.out.println("\tpc " + pc);
        System.out.println("\tmemory:");
        for (int i = 0; i < numMemory; i++) {
            System.out.println("\t\tmem[ " + i + " ] " + mem[i]);
        }
        System.out.println("\tregisters:");
        for (int i = 0; i < NUMREGS; i++) {
            System.out.println("\t\treg[ " + i + " ] " + reg[i]);
        }
        System.out.println("end state");
    }

    private void exit(int code) {
        System.out.println("Error: Exiting with code " + code);
        System.exit(code);
    }

    private static class Field {
        int regA;
        int regB;
        int rd;
        int offsetField;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("error: usage: java SMC <machine-code file>");
            System.exit(1);
        }

        Simulator processor = new Simulator();

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
