import java.util.*;
import static java.util.stream.Collectors.*;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

// FIXME: You should limit your implementation to this class. You are free to add new auxilliary classes. You do not need to touch the LoopNext.g4 file.
class Analysis extends LoopNestBaseListener {

    // Possible types
    enum Types {
        Byte, Short, Int, Long, Char, Float, Double, Boolean, String
    }

    // Type of variable declaration
    enum VariableType {
        Primitive, Array, Literal
    }

    // Types of caches supported
    enum CacheTypes {
        DirectMapped, SetAssociative, FullyAssociative,
    }

    // auxilliary data-structure for converting strings
    // to types, ignoring strings because string is not a
    // valid type for loop bounds
    final Map<String, Types> stringToType = Collections.unmodifiableMap(new HashMap<String, Types>() {
        private static final long serialVersionUID = 1L;

        {
            put("byte", Types.Byte);
            put("short", Types.Short);
            put("int", Types.Int);
            put("long", Types.Long);
            put("char", Types.Char);
            put("float", Types.Float);
            put("double", Types.Double);
            put("boolean", Types.Boolean);
        }
    });

    // auxilliary data-structure for mapping types to their byte-size
    // size x means the actual size is 2^x bytes, again ignoring strings
    final Map<Types, Integer> typeToSize = Collections.unmodifiableMap(new HashMap<Types, Integer>() {
        private static final long serialVersionUID = 1L;

        {
            put(Types.Byte, 0);
            put(Types.Short, 1);
            put(Types.Int, 2);
            put(Types.Long, 3);
            put(Types.Char, 1);
            put(Types.Float, 2);
            put(Types.Double, 3);
            put(Types.Boolean, 0);
        }
    });

    // Map of cache type string to value of CacheTypes
    final Map<String, CacheTypes> stringToCacheType = Collections.unmodifiableMap(new HashMap<String, CacheTypes>() {
        private static final long serialVersionUID = 1L;

        {
            put("FullyAssociative", CacheTypes.FullyAssociative);
            put("SetAssociative", CacheTypes.SetAssociative);
            put("DirectMapped", CacheTypes.DirectMapped);
        }
    });

    // Custom Variables Start

    HashMap<String, Integer> variableVals = new HashMap<String, Integer>();
    Integer cachePower = 0;
    Integer blockPower = 0;
    Integer setSize = 0;
    Integer numSets = 0;
    String cacheType = "";
    String incomingVariable = ""; // set in variableDeclaratorId
    String incomingVariableType = ""; // set in unannPrimitiveType

    HashMap<String, Vector<Integer>> arrayDetails = new HashMap<String, Vector<Integer>>(); // name: size, dims,
                                                                                            // dim1_len, dim2_len,
                                                                                            // dim3_len
    Vector<String> unusedExprNames = new Vector<String>(); // set in expressionName

    Integer forLevel = 0;
    Integer for1Stride = -1;
    Integer for2Stride = -1;
    Integer for3Stride = -1;
    Integer for4Stride = -1;
    Integer for1Max = -1;
    Integer for2Max = -1;
    Integer for3Max = -1;
    Integer for4Max = -1;
    String for1Iterator = "";
    String for2Iterator = "";
    String for3Iterator = "";
    String for4Iterator = "";

    // HashMap<String, Vector<Integer>> arrayAccessDetails = new HashMap<String,
    // Vector<Integer>>(); // name: num of
    // accesses, dims of
    // each access
    // sequentially

    Vector<HashMap<String, Vector<Integer>>> allAccessDetails = new Vector<HashMap<String, Vector<Integer>>>();

    HashMap<String, Long> missesPerTestcase = new HashMap<String, Long>();

    List<HashMap<String, Long>> objToSerialize = new ArrayList<HashMap<String, Long>>();

    // Custom Variables End

    public Analysis() {
    }

    // FIXME: Feel free to override additional methods from
    // LoopNestBaseListener.java based on your needs.
    // Method entry callback
    @Override
    public void enterMethodDeclaration(LoopNestParser.MethodDeclarationContext ctx) {
        // System.out.println("enterMethodDeclaration");
        allAccessDetails.clear();
        for (int i = 0; i < 4; i++) {
            HashMap<String, Vector<Integer>> temp = new HashMap<String, Vector<Integer>>();
            allAccessDetails.add(temp);
        }

    }

    @Override
    public void exitMethodDeclaration(LoopNestParser.MethodDeclarationContext ctx) { // cleanup
        // System.out.println("exitMethodDeclaration");

        HashMap<String, Long> toAdd = new HashMap<String, Long>();

        for (HashMap.Entry<String, Long> entry : missesPerTestcase.entrySet()) {
            String key = entry.getKey();
            Long value = entry.getValue();
            toAdd.put(key, value);
        }

        System.out.println(toAdd);

        // objToSerialize.add(toAdd);

        variableVals.clear();
        for (int i = 0; i < 4; i++)
            allAccessDetails.get(i).clear();
        cachePower = 0;
        blockPower = 0;
        setSize = 0;
        numSets = 0;
        cacheType = "";
        incomingVariable = ""; // set in variableDeclaratorId
        incomingVariableType = ""; // set in unannPrimitiveType

        arrayDetails.clear(); // name: size, dims, dim1_len, dim2_len, dim3_len
        unusedExprNames.clear(); // set in expressionName

        forLevel = 0;
        for1Stride = -1;
        for2Stride = -1;
        for3Stride = -1;
        for4Stride = -1;
        for1Max = -1;
        for2Max = -1;
        for3Max = -1;
        for4Max = -1;
        for1Iterator = "";
        for2Iterator = "";
        for3Iterator = "";
        for4Iterator = "";

        // arrayAccessDetails.clear(); // name: num of accesses, dims of each access
        // sequentially

        missesPerTestcase.clear();

    }

    // End of testcase
    @Override
    public void exitMethodDeclarator(LoopNestParser.MethodDeclaratorContext ctx) {
        // System.out.println("exitMethodDeclarator");
    }

    @Override
    public void exitTests(LoopNestParser.TestsContext ctx) {
        // try {
        //     FileOutputStream fos = new FileOutputStream("Results.obj");
        //     ObjectOutputStream oos = new ObjectOutputStream(fos);
        //     // FIXME: Serialize your data to a file
        //     System.out.println(objToSerialize);
        //     oos.writeObject(objToSerialize);
        //     oos.close();
        // } catch (Exception e) {
        //     throw new RuntimeException(e.getMessage());
        // }
    }

    @Override
    public void exitLocalVariableDeclaration(LoopNestParser.LocalVariableDeclarationContext ctx) {
        incomingVariable = "";
        incomingVariableType = "";
        unusedExprNames.clear();
    }

    @Override
    public void exitVariableDeclarator(LoopNestParser.VariableDeclaratorContext ctx) {
    }

    @Override
    public void exitArrayCreationExpression(LoopNestParser.ArrayCreationExpressionContext ctx) {

    }

    @Override
    public void exitDimExprs(LoopNestParser.DimExprsContext ctx) {
    }

    @Override
    public void exitDimExpr(LoopNestParser.DimExprContext ctx) {
        TerminalNode integerDim = ctx.IntegerLiteral();
        Integer dimension;
        if (integerDim != null)
            dimension = Integer.parseInt(integerDim.getText()); // [<an integer>]
        else { // [<expression>]
            dimension = variableVals.get(unusedExprNames.get(unusedExprNames.size() - 1));
            unusedExprNames.removeElementAt(unusedExprNames.size() - 1);
        }

        if (arrayDetails.containsKey(incomingVariable)) { // >1st dimension
            Vector<Integer> data = arrayDetails.get(incomingVariable);
            data.set(1, data.get(1) + 1);
            data.add((int) (Math.log(dimension) / Math.log(2)));
            arrayDetails.put(incomingVariable, data);
        } else { // 1st dimension
            Vector<Integer> data = new Vector<Integer>();
            data.add(typeToSize.get(stringToType.get(incomingVariableType))); // size of matrix entry
            data.add(1);
            data.add((int) (Math.log(dimension) / Math.log(2)));
            arrayDetails.put(incomingVariable, data);
        }

    }

    @Override
    public void exitLiteral(LoopNestParser.LiteralContext ctx) {
        if (incomingVariable.equals("cachePower")) {
            cachePower = Integer.parseInt(ctx.IntegerLiteral().getText());
        } else if (incomingVariable.equals("blockPower")) {
            blockPower = Integer.parseInt(ctx.IntegerLiteral().getText());
        } else if (incomingVariable.equals("setSize")) {
            setSize = Integer.parseInt(ctx.IntegerLiteral().getText());
            setSize = (int) (Math.log(setSize) / Math.log(2));
        } else if (incomingVariable.equals("cacheType")) {
            cacheType = ctx.StringLiteral().getText();
        } else if (ctx.StringLiteral() == null && !incomingVariable.equals("")) { // any integral variable is saved to
                                                                                  // the hashmap, TODO: Make more
                                                                                  // generalized
            if (ctx.IntegerLiteral() != null)
                variableVals.put(incomingVariable, Integer.parseInt(ctx.IntegerLiteral().getText()));
            else if (ctx.FloatingPointLiteral() != null)
                variableVals.put(incomingVariable, (int) Double.parseDouble(ctx.FloatingPointLiteral().getText()));
            else if (ctx.CharacterLiteral() != null)
                variableVals.put(incomingVariable, (int) (ctx.CharacterLiteral().getText().charAt(0)));
            else if (ctx.BooleanLiteral() != null)
                variableVals.put(incomingVariable, ctx.BooleanLiteral().getText().equals("true") ? 1 : 0);
        }
    }

    @Override
    public void exitVariableDeclaratorId(LoopNestParser.VariableDeclaratorIdContext ctx) {
        TerminalNode variableDeclaratorId = ctx.Identifier();
        String varName = variableDeclaratorId.getText();
        if (forLevel == 0) {
            incomingVariable = varName;
        } else if (forLevel == 1 && for1Iterator.equals("")) {
            for1Iterator = varName;
        } else if (forLevel == 2 && for2Iterator.equals("")) {
            for2Iterator = varName;
        } else if (forLevel == 3 && for3Iterator.equals("")) {
            for3Iterator = varName;
        } else if (forLevel == 4 && for4Iterator.equals("")) {
            for4Iterator = varName;
        } else if (forLevel == 1) {
            incomingVariable = varName;
        } else if (forLevel == 2) {
            incomingVariable = varName;
        } else if (forLevel == 3) {
            incomingVariable = varName;
        } else if (forLevel == 4) {
            incomingVariable = varName;
        }
    }

    @Override
    public void exitUnannArrayType(LoopNestParser.UnannArrayTypeContext ctx) {
    }

    @Override
    public void enterDims(LoopNestParser.DimsContext ctx) {
    }

    @Override
    public void exitUnannPrimitiveType(LoopNestParser.UnannPrimitiveTypeContext ctx) {
        incomingVariableType = ctx.getText();
    }

    @Override
    public void exitNumericType(LoopNestParser.NumericTypeContext ctx) {
    }

    @Override
    public void exitIntegralType(LoopNestParser.IntegralTypeContext ctx) {
    }

    @Override
    public void exitFloatingPointType(LoopNestParser.FloatingPointTypeContext ctx) {
    }

    // TODO: Extend to other types

    @Override
    public void exitExpressionName(LoopNestParser.ExpressionNameContext ctx) {
        TerminalNode expressionName = ctx.Identifier();
        unusedExprNames.add(expressionName.getText());
    }

    @Override
    public void exitForInit(LoopNestParser.ForInitContext ctx) {
    }

    @Override
    public void exitForCondition(LoopNestParser.ForConditionContext ctx) {
    }

    @Override
    public void exitRelationalExpression(LoopNestParser.RelationalExpressionContext ctx) {
        TerminalNode relationalExpression = ctx.IntegerLiteral();
        Integer forMax;

        if (relationalExpression != null) {
            forMax = Integer.parseInt(relationalExpression.getText());
        } else {

            forMax = variableVals.get(unusedExprNames.get(unusedExprNames.size() - 1)); // should be the [1] element
            unusedExprNames.removeElementAt(unusedExprNames.size() - 1);
        }

        unusedExprNames.removeElementAt(unusedExprNames.size() - 1); // shoukd empty tge vector completely

        if (forLevel == 1 && for1Max == -1)
            for1Max = forMax;
        if (forLevel == 2 && for2Max == -1)
            for2Max = forMax;
        if (forLevel == 3 && for3Max == -1)
            for3Max = forMax;
        if (forLevel == 4 && for4Max == -1)
            for4Max = forMax;

    }

    @Override
    public void exitForUpdate(LoopNestParser.ForUpdateContext ctx) {
    }

    @Override
    public void exitSimplifiedAssignment(LoopNestParser.SimplifiedAssignmentContext ctx) {
        TerminalNode simplifiedAssignment = ctx.IntegerLiteral();
        Integer forStride;
        if (simplifiedAssignment != null) {
            forStride = Integer.parseInt(simplifiedAssignment.getText());
        } else {
            forStride = variableVals.get(unusedExprNames.get(unusedExprNames.size() - 1)); // should be the [1] element
            unusedExprNames.removeElementAt(unusedExprNames.size() - 1);
        }
        unusedExprNames.removeElementAt(unusedExprNames.size() - 1); // shoukd empty tge vector completely

        if (forLevel == 1 && for1Stride == -1) {
            for1Stride = forStride;
            for1Max = (int) (Math.log(for1Max) / Math.log(2));
            for1Stride = (int) (Math.log(for1Stride) / Math.log(2));
        }
        if (forLevel == 2 && for2Stride == -1) {
            for2Stride = forStride;
            for2Max = (int) (Math.log(for2Max) / Math.log(2));
            for2Stride = (int) (Math.log(for2Stride) / Math.log(2));
        }
        if (forLevel == 3 && for3Stride == -1) {
            for3Stride = forStride;
            for3Max = (int) (Math.log(for3Max) / Math.log(2));
            for3Stride = (int) (Math.log(for3Stride) / Math.log(2));
        }
        if (forLevel == 4 && for4Stride == -1) {
            for4Stride = forStride;
            for4Max = (int) (Math.log(for4Max) / Math.log(2));
            for4Stride = (int) (Math.log(for4Stride) / Math.log(2));
        }
    }

    @Override
    public void enterArrayAccess(LoopNestParser.ArrayAccessContext ctx) {
        unusedExprNames.clear(); // anything other than array elements are of no use in cache miss calc. TODO:
                                 // possible source of bugs.
    }

    @Override
    public void exitArrayAccess(LoopNestParser.ArrayAccessContext ctx) {
        // System.out.println("ArrayAccess");
        String arrayName = unusedExprNames.get(0);
        int dims = arrayDetails.get(arrayName).get(1);

        Vector<Integer> data = new Vector<Integer>();

        // if (arrayAccessDetails.containsKey(arrayName)) {
        // data = arrayAccessDetails.get(arrayName);
        // data.set(0, data.get(0) + 1);
        // } else {
        // data = new Vector<Integer>();
        // data.add(1);
        // }

        for (int i = 0; i < forLevel; i++) {
            if (allAccessDetails.get(i).containsKey(arrayName))
                allAccessDetails.get(i).remove(arrayName);
        }

        for (int i = 0; i < dims; i++) {
            String iterator = unusedExprNames.get(1 + i);
            if (iterator.equals(for1Iterator))
                data.add(1);
            else if (iterator.equals(for2Iterator))
                data.add(2);
            else if (iterator.equals(for3Iterator))
                data.add(3);
            else if (iterator.equals(for4Iterator))
                data.add(4);
            else
                data.add(-1);
        }

        allAccessDetails.get(forLevel - 1).put(arrayName, data);

        // computeMisses(arrayName);

    }

    @Override
    public void enterForStatement(LoopNestParser.ForStatementContext ctx) {
        forLevel += 1;
    }

    @Override
    public void exitForStatement(LoopNestParser.ForStatementContext ctx) {
        computeMissesAfterFor(forLevel);
        forLevel -= 1;
    }

    @Override
    public void exitAssignment(LoopNestParser.AssignmentContext ctx) {
        unusedExprNames.clear();
    }

    @Override
    public void enterArrayAccess_lfno_primary(LoopNestParser.ArrayAccess_lfno_primaryContext ctx) {
        unusedExprNames.clear(); // anything other than array elements are of no use in cache miss calc. TODO:
                                 // possible source of bugs.
    }

    @Override
    public void exitArrayAccess_lfno_primary(LoopNestParser.ArrayAccess_lfno_primaryContext ctx) {
        // System.out.println("ArrayAccess_lfno_primary");
        String arrayName = unusedExprNames.get(0);
        int dims = arrayDetails.get(arrayName).get(1);

        Vector<Integer> data = new Vector<Integer>();

        // if (arrayAccessDetails.containsKey(arrayName)) {
        // data = arrayAccessDetails.get(arrayName);
        // data.set(0, data.get(0) + 1);
        // } else {
        // data = new Vector<Integer>();
        // data.add(1);
        // }

        for (int i = 0; i < forLevel; i++) {
            if (allAccessDetails.get(i).containsKey(arrayName))
                allAccessDetails.get(i).remove(arrayName);
        }

        for (int i = 0; i < dims; i++) {
            String iterator = unusedExprNames.get(1 + i);
            ;
            if (iterator.equals(for1Iterator))
                data.add(1);
            else if (iterator.equals(for2Iterator))
                data.add(2);
            else if (iterator.equals(for3Iterator))
                data.add(3);
            else if (iterator.equals(for4Iterator))
                data.add(4);
            else
                data.add(-1);
        }

        allAccessDetails.get(forLevel - 1).put(arrayName, data);

        // computeMisses(arrayName);
    }

    void computeMissesAfterFor(Integer level) {

        for (HashMap.Entry<String, Vector<Integer>> entry : allAccessDetails.get(level - 1).entrySet()) {
            String key = entry.getKey();
            Vector<Integer> value = entry.getValue();
            computeMisses(key, value);
        }

    }

    void computeMisses(String arrayName, Vector<Integer> accessInfo) {
        // TODO: implement for multiple accesses to same array
        Vector<Integer> info = arrayDetails.get(arrayName);
        // Vector<Integer> accessInfo = arrayAccessDetails.get(arrayName);
        Integer entriesPerBlock = blockPower - info.get(0);
        Integer arrayDims = info.get(1);

        Integer misses = 0;

        if (cacheType.equals("\"DirectMapped\"")) {
            numSets = cachePower - blockPower;
            setSize = 0;
        } else if (cacheType.equals("\"FullyAssociative\"")) {
            numSets = 0;
            setSize = cachePower - blockPower;
        } else if (cacheType.equals("\"SetAssociative\""))
            numSets = cachePower - blockPower - setSize;
        // else {
        //     System.out.println("ERRRRRRRORRRRRRRRRRRRRRRRRRR");
        // }

        if (entriesPerBlock < 0) {
            numSets += entriesPerBlock;
            entriesPerBlock = 0;
        }

        Integer blockBits = entriesPerBlock;
        Integer setBits = numSets;
        Integer tagBits = -blockBits - setBits;
        for (int i = 0; i < arrayDims; i++)
            tagBits += info.get(2 + i);
        // ttttttt...ttttttttsssssssss...ssssssssbbbbbbbb...bbbbbbbb
        // if #tagbits accessed > setSize => overwrite happens

        // Checking Overwrite Start

        Integer overwrite1 = -1;
        Integer overwrite2 = -1;
        Integer overwrite3 = -1;
        Integer overwrite4 = -1;

        Integer overallTagBitsAccessed = 0;

        if (forLevel >= 4) {

            Integer dimensionSameAsForIterator = -1;
            for (int i = 0; i < arrayDims; i++)
                if (accessInfo.get(i) == 4)
                    dimensionSameAsForIterator = i;

            if (dimensionSameAsForIterator == -1)
                overwrite4 = 0;
            else {
                Integer leastBitChanged = for4Stride;
                for (int i = 1 + dimensionSameAsForIterator; i < arrayDims; i++)
                    leastBitChanged += info.get(2 + i);
                // if a[k][j][i] accessed and iterator is k, we add stride for k and dimension
                // for j and i for leastbitchanged.

                Integer greatestBitChanged = for4Max - 1;
                for (int i = 1 + dimensionSameAsForIterator; i < arrayDims; i++)
                    greatestBitChanged += info.get(2 + i);

                Integer tagBitsAccessed = -1;
                if (blockBits + setBits > greatestBitChanged)
                    tagBitsAccessed = 0;
                else if (blockBits + setBits < leastBitChanged)
                    tagBitsAccessed = (greatestBitChanged - leastBitChanged + 1);
                else
                    tagBitsAccessed = greatestBitChanged - (blockBits + setBits) + 1;

                overallTagBitsAccessed += tagBitsAccessed;

                if (overallTagBitsAccessed > setSize)
                    overwrite4 = 1;
                else
                    overwrite4 = 0;
            }
        }
        // System.out.println(overallTagBitsAccessed);

        if (forLevel >= 3) {

            if (overwrite4 == 1)
                overwrite3 = 1;
            else {
                Integer dimensionSameAsForIterator = -1;
                for (int i = 0; i < arrayDims; i++)
                    if (accessInfo.get(i) == 3)
                        dimensionSameAsForIterator = i;

                if (dimensionSameAsForIterator == -1)
                    overwrite3 = 0;
                else {
                    Integer leastBitChanged = for3Stride;
                    for (int i = 1 + dimensionSameAsForIterator; i < arrayDims; i++)
                        leastBitChanged += info.get(2 + i);
                    // if a[k][j][i] accessed and iterator is k, we add stride for k and dimension
                    // for j and i for leastbitchanged.

                    Integer greatestBitChanged = for3Max - 1;
                    for (int i = 1 + dimensionSameAsForIterator; i < arrayDims; i++)
                        greatestBitChanged += info.get(2 + i);

                    Integer tagBitsAccessed = -1;
                    if (blockBits + setBits > greatestBitChanged)
                        tagBitsAccessed = 0;
                    else if (blockBits + setBits < leastBitChanged)
                        tagBitsAccessed = (greatestBitChanged - leastBitChanged + 1);
                    else
                        tagBitsAccessed = greatestBitChanged - (blockBits + setBits) + 1;

                    overallTagBitsAccessed += tagBitsAccessed;

                    if (overallTagBitsAccessed > setSize)
                        overwrite3 = 1;
                    else
                        overwrite3 = 0;
                }
            }
        }
        // System.out.println(overallTagBitsAccessed);

        if (forLevel >= 2) {
            if (overwrite3 == 1)
                overwrite2 = 1;
            else {
                Integer dimensionSameAsForIterator = -1;
                for (int i = 0; i < arrayDims; i++)
                    if (accessInfo.get(i) == 2)
                        dimensionSameAsForIterator = i;
                if (dimensionSameAsForIterator == -1)
                    overwrite2 = 0;
                else {
                    Integer leastBitChanged = for2Stride;
                    for (int i = 1 + dimensionSameAsForIterator; i < arrayDims; i++)
                        leastBitChanged += info.get(2 + i);
                    // if a[k][j][i] accessed and iterator is k, we add stride for k and dimension
                    // for j and i for leastbitchanged.

                    Integer greatestBitChanged = for2Max - 1;
                    for (int i = 1 + dimensionSameAsForIterator; i < arrayDims; i++)
                        greatestBitChanged += info.get(2 + i);
                    Integer tagBitsAccessed = -1;
                    if (blockBits + setBits > greatestBitChanged)
                        tagBitsAccessed = 0;
                    else if (blockBits + setBits < leastBitChanged)
                        tagBitsAccessed = (greatestBitChanged - leastBitChanged + 1);
                    else
                        tagBitsAccessed = greatestBitChanged - (blockBits + setBits) + 1;

                    overallTagBitsAccessed += tagBitsAccessed;

                    if (overallTagBitsAccessed > setSize)
                        overwrite2 = 1;
                    else
                        overwrite2 = 0;

                }
            }
        }

        // System.out.println(overallTagBitsAccessed);
        // System.out.println(setSize);

        if (forLevel >= 1) {
            if (overwrite2 == 1)
                overwrite1 = 1;
            else {
                Integer dimensionSameAsForIterator = -1;
                for (int i = 0; i < arrayDims; i++)
                    if (accessInfo.get(i) == 1)
                        dimensionSameAsForIterator = i;

                if (dimensionSameAsForIterator == -1)
                    overwrite1 = 0;
                else {
                    Integer leastBitChanged = for1Stride;
                    for (int i = 1 + dimensionSameAsForIterator; i < arrayDims; i++)
                        leastBitChanged += info.get(2 + i);
                    // if a[k][j][i] accessed and iterator is k, we add stride for k and dimension
                    // for j and i for leastbitchanged.

                    Integer greatestBitChanged = for1Max - 1;
                    for (int i = 1 + dimensionSameAsForIterator; i < arrayDims; i++)
                        greatestBitChanged += info.get(2 + i);

                    Integer tagBitsAccessed = -1;
                    if (blockBits + setBits > greatestBitChanged)
                        tagBitsAccessed = 0;
                    else if (blockBits + setBits < leastBitChanged)
                        tagBitsAccessed = (greatestBitChanged - leastBitChanged + 1);
                    else
                        tagBitsAccessed = greatestBitChanged - (blockBits + setBits) + 1;

                    overallTagBitsAccessed += tagBitsAccessed;

                    if (overallTagBitsAccessed > setSize) {
                        overwrite1 = 1;
                    } else
                        overwrite1 = 0;

                }
            }

        }
        // System.out.println(overallTagBitsAccessed);

        // Checking Overwrite End

        Vector<Integer> flatStrides = new Vector<Integer>();
        Vector<Integer> maxVector = new Vector<Integer>();
        Vector<Integer> strideVector = new Vector<Integer>();
        Vector<Integer> overwriteVector = new Vector<Integer>();

        if (forLevel >= 1)
            maxVector.insertElementAt(for1Max, 0);
        if (forLevel >= 2)
            maxVector.insertElementAt(for2Max, 0);
        if (forLevel >= 3)
            maxVector.insertElementAt(for3Max, 0);
        if (forLevel >= 4)
            maxVector.insertElementAt(for4Max, 0);
        if (forLevel >= 1)
            strideVector.insertElementAt(for1Stride, 0);
        if (forLevel >= 2)
            strideVector.insertElementAt(for2Stride, 0);
        if (forLevel >= 3)
            strideVector.insertElementAt(for3Stride, 0);
        if (forLevel >= 4)
            strideVector.insertElementAt(for4Stride, 0);
        if (forLevel >= 1)
            overwriteVector.insertElementAt(overwrite1, 0);
        if (forLevel >= 2)
            overwriteVector.insertElementAt(overwrite2, 0);
        if (forLevel >= 3)
            overwriteVector.insertElementAt(overwrite3, 0);
        if (forLevel >= 4)
            overwriteVector.insertElementAt(overwrite4, 0);

        Integer mostSignificantDimensionAccessed = 100;
        Integer isOverwrittenYet = 0;

        // System.out.println("Overwrite");
        // System.out.println(overwriteVector);

        for (int i = 0; i < forLevel; i++) {

            Integer flattenedStride = strideVector.get(i);
            Integer dimensionSameAsForIterator = 100;
            for (int j = 0; j < arrayDims; j++) {
                if (accessInfo.get(j) == (forLevel - i))
                    dimensionSameAsForIterator = j;
            }
            if (dimensionSameAsForIterator == 100) {
                if (isOverwrittenYet == 1)
                    misses += (maxVector.get(i) - strideVector.get(i));
                // misses repeated if overwritten, otherwise no addition to misses
            } else {
                for (int j = dimensionSameAsForIterator + 1; j < arrayDims; j++)
                    flattenedStride += info.get(2 + j); // add size of lower dimensions

                // if (dimensionSameAsForIterator==arrayDims-1) { // last dimension
                // if (flattenedStride >= entriesPerBlock)
                // misses += Math.max(maxVector.get(i) - strideVector.get(i), 0);
                // else
                // misses += Math.max(maxVector.get(i) - entriesPerBlock, 0);
                // }
                // else if (arrayDims > 1 && dimensionSameAsForIterator==arrayDims-2){
                // if (entriesPerBlock <= flattenedStride)
                // misses += Math.max(maxVector.get(i) - strideVector.get(i), 0);
                // else
                // misses += Math.max(maxVector.get(i) -
                // (entriesPerBlock-flattenedStride+strideVector.get(i)), 0);
                // }
                // else if (arrayDims > 2 && dimensionSameAsForIterator==arrayDims-3){
                if (entriesPerBlock > flattenedStride && isOverwrittenYet == 0)
                    misses += Math.max(maxVector.get(i) - (entriesPerBlock - flattenedStride + strideVector.get(i)), 0);
                else
                    misses += Math.max(maxVector.get(i) - strideVector.get(i), 0);

                // }

                // if (mostSignificantDimensionAccessed<dimensionSameAsForIterator &&
                // isOverwrittenYet==0) {
                // misses -= Math.max(entriesPerBlock-flattenedStride,0);
                // System.out.println("subtractmisses");
                // System.out.println(flattenedStride);
                // }
                mostSignificantDimensionAccessed = Math.min(mostSignificantDimensionAccessed,
                        dimensionSameAsForIterator);
                isOverwrittenYet = overwriteVector.get(i);
            }
            // System.out.println("InFor");
            // System.out.println(maxVector.get(i));
            // System.out.println(strideVector.get(i));
            // System.out.println(entriesPerBlock);
            // System.out.println(flattenedStride);
            // System.out.println(mostSignificantDimensionAccessed);
            // System.out.println(misses);

        }

        // System.out.println(Math.pow(2, misses));

        missesPerTestcase.put(arrayName, (long) (Math.pow(2, misses)));

        // if (forLevel >= 1) {
        // for1FlatStride = for1Stride;
        // if (accessInfo.get(arrayDims) == 1) { // last dimension
        // if (for1Stride >= entriesPerBlock)
        // misses += Math.max(for1Max - for1Stride, 0);
        // else
        // misses += Math.max(for1Max - entriesPerBlock, 0);
        // } else if (arrayDims > 1 && accessInfo.get(arrayDims - 1) == 1) {
        // if (entriesPerBlock <= info.get(arrayDims + 1))
        // misses += Math.max(for1Max - for1Stride, 0);
        // else
        // misses += Math.max(for1Max - (entriesPerBlock - info.get(arrayDims + 1)), 0);
        // } else if (arrayDims > 2 && accessInfo.get(arrayDims - 2) == 1) {
        // if (entriesPerBlock <= (info.get(arrayDims + 1) + info.get(arrayDims)))
        // misses += Math.max(for1Max - for1Stride, 0);
        // else
        // misses += Math.max(for1Max - (entriesPerBlock - info.get(arrayDims + 1) -
        // info.get(arrayDims)), 0);
        // }
        // }
        // System.out.println(arrayName);
        // System.out.println("-------------------------");
    }
}
