package wvw.mobile.rules.explanation;

import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Derivation;
import com.hp.hpl.jena.reasoner.rulesys.RuleDerivation;
import com.hp.hpl.jena.util.PrintUtil;

public class ExplanationRunner {

    public static void print(String message) {
        Log.d("Explanation-Runner", message);
    }


    public static void runContextualExplanationTest(){
        // Set-up rules
        String rules = "[transitiveRule: (?a ex:equals ?b) (?b ex:equals ?c) -> (?a ex:equals ?c)]";

        // Create and set-up the Explainer
        Explainer explainer = new Explainer();
        explainer.Model(ModelFactory.getTransitiveBaseModel());
        explainer.Rules(rules);

        // Generate the contextual explanation.
        String results = "Transitive_Explainer -- ContextualExplanation\n";
        results += explainer.GetShallowContextualExplanation(
                explainer.Model().getResource("A"),
                explainer.Model().getProperty("http://example.com/equals"),
                explainer.Model().getResource("D")
        );

        results += "\n";

        results += explainer.GetSimpleContextualExplanation(
                explainer.Model().getResource("A"),
                explainer.Model().getProperty("http://example.com/equals"),
                explainer.Model().getResource("D")
        );

        print(results + "\n");
    }

    public static void runCounterfactualExplanationTest(){

        // Set-up the Explainer
        Explainer explainer2 = new Explainer();
        explainer2.Model(ModelFactory.getFoodRecommendationBaseModel());
        explainer2.Rules(ModelFactory.getFoodRecommendationRules());

        // Set-up the Additional Model needed to run the counterfactual explanation
        InfModel infModel = ModelFactory.getFoodRecommendationInfModel();

        Resource person  = infModel.getResource(ModelFactory.getPersonURI());
        Property totalSugars = infModel.getProperty("http://example.com/totalSugars");

        StmtIterator itr = infModel.listStatements(person, totalSugars, (RDFNode) null);

        // Use the Explainer to generate a counterfactual explanation.
        String res = "FoodRecommendation_Explainer -- CounterfactualExplanation\n";
        while(itr.hasNext()) {
            Statement s = itr.next();
            res += explainer2.GetFullCounterfactualExplanation(s, ModelFactory.getFoodRecommendationBaseModelBanana());
        }
        print(res);
    }

    public static void runTraceBasedExplanationTest(){

        // Set up the Explainer
        String rules = "[transitiveRule: (?a ex:equals ?b) (?b ex:equals ?c) -> (?a ex:equals ?c)]";
        Explainer explainer = new Explainer();
        explainer.Model(ModelFactory.getTransitiveBaseModel());
        explainer.Rules(rules);

        // Demonstrate a trace-based explanation.
        String traceResponse = explainer.GetFullTracedBaseExplanation(explainer.Model().getResource("A"),
                explainer.Model().getProperty("http://example.com/equals"),
                explainer.Model().getResource("D"));
        print(traceResponse);
    }

    public static void runTraceBasedExplanationTestFoodRecommendation() {
        print("Running Trace-Based Explanation Test on FoodRecommendation...");

        // Set up the Explainer
        Explainer explainer = new Explainer();

        // Set up the Model with namespaces
        Model model = ModelFactory.getFoodRecommendationBaseModel();
        model.setNsPrefix("schema", "http://schema.org/");
        model.setNsPrefix("usda", "http://example.com/usda#");
        model.setNsPrefix("ex", "http://example.com/");
        model.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
        explainer.Model(model);
        print("Base Model contents:");
        StmtIterator baseStmts = explainer.Model().listStatements();
        while(baseStmts.hasNext()) {
            print("\t" + baseStmts.next().toString());
        }

        // Set up Rules
        explainer.Rules(ModelFactory.getFoodRecommendationRulesPrefix());
        print("Rules loaded:");
        print(ModelFactory.getFoodRecommendationRulesPrefix());

        // Check inference model
        // print("Inference Model contents:");
        // InfModel infModel = explainer.generateInfModel(explainer.Model());
        // StmtIterator infStmts = infModel.listStatements();
        // while(infStmts.hasNext()) {
        //     print("\t" + infStmts.next().toString());
        // }

        // Define the data type for double
        TypeMapper tm = TypeMapper.getInstance();
        RDFDatatype xsdDouble = tm.getTypeByName("http://www.w3.org/2001/XMLSchema#double");
        print("\tDefined data types");

        // Demonstrate a trace-based explanation
        String traceResponse;
        try {
            traceResponse = explainer.GetFullTracedBaseExplanation_B(
                    explainer.Model().getResource("http://xmlns.com/foaf/0.1/Person"),
                    explainer.Model().getProperty("http://example.com/totalSugars"),
                    ResourceFactory.createTypedLiteral("20.78", xsdDouble)
            );
        } catch (Exception e) {
            traceResponse = "Error generating explanation: " + e.getMessage();
            Log.e("TraceExplanation", "Error", e);
        }
        print(traceResponse);
    }



    public static void runTraceBasedExplanationTestLoanEligibility() {

        print("Running Trace-Based Explanation Test on Loan Eligibility...");

        // Set up the Explainer
        Explainer explainer = new Explainer();

        // Set up the Base Model
        explainer.Model(ModelFactory.getLoanEligibilityBaseModel());
        print("Base Model contents:");
        StmtIterator baseStmts = explainer.Model().listStatements();
        while(baseStmts.hasNext()) {
            print("\t" + baseStmts.next().toString());
        }

        // Set up the Rules
        explainer.Rules(ModelFactory.getLoanEligibilityRules());
        print("Rules loaded:");
        print(ModelFactory.getLoanEligibilityRules());

        // Define the RDFDatatype for double
        TypeMapper tm = TypeMapper.getInstance();
        RDFDatatype xsdDouble = tm.getTypeByName("http://www.w3.org/2001/XMLSchema#double");

        // Set up the resource for the applicant
        Resource applicant = explainer.Model().getResource("http://xmlns.com/foaf/0.1/Person");

        // Define the property for debt-to-income ratio (this is what the rules calculate)
        Property debtToIncomeRatioProperty = explainer.Model().getProperty("http://example.com/debtToIncomeRatio");

        print("\tDefined data types");

        // Generate a trace-based explanation for the applicant's loan eligibility
        String traceResponse = explainer.GetFullTracedBaseExplanation_B(
                applicant,
                debtToIncomeRatioProperty,
                ResourceFactory.createTypedLiteral("0.30", xsdDouble));  // Assume a debt-to-income ratio for testing

        // Print the trace-based explanation
        print(traceResponse);
    }


    public static void runContextualExplanationTestFoodRecommendation(){

        print("Running Contextual Explanation Test on FoodRecommendation...");

        // Create and set-up the Explainer
        Explainer explainer = new Explainer();
        explainer.Model(ModelFactory.getFoodRecommendationBaseModel());
        explainer.Rules(ModelFactory.getFoodRecommendationRulesPrefix());

        // Add debug logging
        Log.d("Explanation-Runner", "Base Model contents:");
        StmtIterator baseStmts = explainer.Model().listStatements();
        while(baseStmts.hasNext()) {
            Log.d("Explanation-Runner", "\t" + baseStmts.next().toString());
        }

        Log.d("Explanation-Runner", "Rules:");
        Log.d("Explanation-Runner", explainer.Rules());

        TypeMapper tm = TypeMapper.getInstance();
        RDFDatatype xsdDouble = tm.getTypeByName("http://www.w3.org/2001/XMLSchema#double");

        // Generate the contextual explanation.
        String results = "FoodRecommendation_Explainer -- ContextualExplanation\n";
        results += explainer.GetShallowContextualExplanation(
                explainer.Model().getResource("http://xmlns.com/foaf/0.1/Person"),
                explainer.Model().getProperty("http://example.com/totalSugars"),
                // explainer.Model().getResource("\"20.78\"^^http://www.w3.org/2001/XMLSchema#double")
                ResourceFactory.createTypedLiteral("20.78", xsdDouble)
        );

        results += "\n";

        results += explainer.GetSimpleContextualExplanation(
                explainer.Model().getResource("http://xmlns.com/foaf/0.1/Person"),
                explainer.Model().getProperty("http://example.com/totalSugars"),
                // explainer.Model().getResource("\"20.78\"^^http://www.w3.org/2001/XMLSchema#double")
                ResourceFactory.createTypedLiteral("20.78", xsdDouble)
        );

        print(results + "\n");
    }

    public static void runContextualExplanationTestLoanEligibility(){

        print("Running Contextual Explanation Test on Loan Eligibility...");

        // Create and set-up the Explainer
        Explainer explainer = new Explainer();
        explainer.Model(ModelFactory.getLoanEligibilityBaseModel());
        explainer.Rules(ModelFactory.getLoanEligibilityRules());

        // Add debug logging
        Log.d("Explanation-Runner", "Base Model contents:");
        StmtIterator baseStmts = explainer.Model().listStatements();
        while(baseStmts.hasNext()) {
            Log.d("Explanation-Runner", "\t" + baseStmts.next().toString());
        }

        Log.d("Explanation-Runner", "Rules:");
        Log.d("Explanation-Runner", explainer.Rules());

        TypeMapper tm = TypeMapper.getInstance();
        RDFDatatype xsdDouble = tm.getTypeByName("http://www.w3.org/2001/XMLSchema#double");

        // Generate the contextual explanation.
        String results = "LoanEligibility_Explainer -- ContextualExplanation\n";
        results += explainer.GetShallowContextualExplanation(
                explainer.Model().getResource("http://xmlns.com/foaf/0.1/Person"),
                explainer.Model().getProperty("http://example.com/debtToIncomeRatio"),
                // explainer.Model().getResource("\"20.78\"^^http://www.w3.org/2001/XMLSchema#double")
                ResourceFactory.createTypedLiteral("0.30", xsdDouble)
        );

        results += "\n";

        results += explainer.GetSimpleContextualExplanation(
                explainer.Model().getResource("http://xmlns.com/foaf/0.1/Person"),
                explainer.Model().getProperty("http://example.com/debtToIncomeRatio"),
                // explainer.Model().getResource("\"20.78\"^^http://www.w3.org/2001/XMLSchema#double")
                ResourceFactory.createTypedLiteral("0.30", xsdDouble)
        );

        print(results + "\n");
    }

    public static void runContrastiveExplanationTestFoodRecommendation(){

        print("Running Contrastive Explanation Test on FoodRecommendation...");

        // Set-up the Explainer
        Explainer explainer2 = new Explainer();
        explainer2.Model(ModelFactory.getFoodRecommendationBaseModel());
        explainer2.Rules(ModelFactory.getFoodRecommendationRulesPrefix());

        // Add debug logging
        Log.d("Explanation-Runner", "Base Model contents:");
        StmtIterator baseStmts = explainer2.Model().listStatements();
        while(baseStmts.hasNext()) {
            Log.d("Explanation-Runner", "\t" + baseStmts.next().toString());
        }

        Log.d("Explanation-Runner", "Rules:");
        Log.d("Explanation-Runner", explainer2.Rules());

        // Set-up the Additional Model needed to run the counterfactual explanation
        InfModel infModel = ModelFactory.getFoodRecommendationInfModel();

        Log.d("Explanation-Runner", "Inference Model contents:");
        StmtIterator infStmts = infModel.listStatements();
        while(infStmts.hasNext()) {
            Log.d("Explanation-Runner", "\t" + infStmts.next().toString());
        }

        // Resource person  = infModel.getResource(ModelFactory.getPersonURI());
        // Property totalSugars = infModel.getProperty("http://example.com/totalSugars");
        Resource observation = infModel.getResource("http://example.com/observation");
        Property allowedToEat = infModel.getProperty("http://example.com/allowedToEat");

        // StmtIterator itr = infModel.listStatements(person, totalSugars, (RDFNode) null);
        StmtIterator itr = infModel.listStatements(observation, allowedToEat, (RDFNode) null);

        // Use the Explainer to generate a counterfactual explanation.
        String result = "FoodRecommendation_Explainer -- ContrastiveExplanation\n";
        while(itr.hasNext()) {
            Statement s = itr.next();
            result += explainer2.GetFullCounterfactualExplanation_B(s, ModelFactory.getFoodRecommendationBaseModelBanana());
        }
        print(result);
    }

    public static void runContrastiveExplanationTestLoanEligibility() {

        Log.d("Explanation-Runner", "Running Contrastive Explanation Test on Loan Eligibility...");

        // Set-up the Explainer
        Explainer explainer = new Explainer();
        explainer.Model(ModelFactory.getLoanEligibilityBaseModel());
        explainer.Rules(ModelFactory.getLoanEligibilityRules());

        // Add debug logging
        Log.d("Explanation-Runner", "Base Model contents:");
        StmtIterator baseStmts = explainer.Model().listStatements();
        while(baseStmts.hasNext()) {
            Log.d("Explanation-Runner", "\t" + baseStmts.next().toString());
        }

        Log.d("Explanation-Runner", "Rules:");
        Log.d("Explanation-Runner", explainer.Rules());

        // Set-up the Additional Model needed to run the counterfactual explanation
        InfModel infModel = ModelFactory.getLoanEligibilityInfModel();

        // Log.d("Explanation-Runner", "Inference Model contents:");
        // StmtIterator infStmts = infModel.listStatements();
        // while(infStmts.hasNext()) {
        //     Log.d("Explanation-Runner", "\t" + infStmts.next().toString());
        // }

        Resource applicant = infModel.getResource(ModelFactory.getPersonURI());
        Property loanEligibility = infModel.getProperty("http://example.com/loanEligibility");

        // List the statements about the applicant's loan eligibility
        StmtIterator itr = infModel.listStatements(applicant, loanEligibility, (RDFNode) null);

        // Use the Explainer to generate a counterfactual explanation.
        String result = "LoanEligibility_Explainer -- Contrastive Explanation\n";
        while (itr.hasNext()) {
            Statement s = itr.next();
            result += explainer.GetFullCounterfactualExplanation_B(s, ModelFactory.getLoanEligibilityBaseModelSecondType());
        }
        Log.d("Explanation-Runner", result);
    }

//    public static void runContrastiveExplanationTestFoodRecommendation() {
//
//        print("Running Contrastive Explanation Test on FoodRecommendation...");
//
//        // Set-up the Explainer
//        Explainer explainer = new Explainer();
//        explainer.Model(ModelFactory.getFoodRecommendationBaseModel());  // Set model
//        explainer.Rules(ModelFactory.getFoodRecommendationRules());      // Set rules
//
//        // Set-up the Additional Model needed to run the contrastive explanation
//        InfModel infModel = ModelFactory.getFoodRecommendationInfModel();
//
//        // Define resources and properties involved in the explanation
//        Resource person = infModel.getResource(ModelFactory.getPersonURI());
//        Property totalSugars = infModel.getProperty("http://example.com/totalSugars");
//
//        // Gather actual facts (the factual statements)
//        StmtIterator factualIterator = infModel.listStatements(person, totalSugars, (RDFNode) null);
//
//        // Assume we want to contrast it with a hypothetical scenario where the person ate less sugar
//        Model contrastiveModel = ModelFactory.getFoodRecommendationBaseModelWithLessSugar();
//
//        // Create a string to store the results of the contrastive explanation
//        String results = "FoodRecommendation_Explainer -- ContrastiveExplanation\n";
//
//        // Iterate over the factual statements and generate contrastive explanations
//        while (factualIterator.hasNext()) {
//            Statement factualStatement = factualIterator.next();
//
//            // Generate the factual explanation
//            String factualExplanation = explainer.GetFullFactualExplanation(factualStatement, ModelFactory.getFoodRecommendationBaseModelBanana());
//
//            // Generate the contrastive explanation by comparing with a hypothetical scenario
//            String contrastiveExplanation = explainer.GetContrastiveExplanation(factualStatement, contrastiveModel);
//
//            // Append both explanations to the result
//            results += "Factual Explanation: \n" + factualExplanation + "\n";
//            results += "Contrastive Explanation: \n" + contrastiveExplanation + "\n\n";
//        }
//
//        // Print the contrastive explanation results
//        print(results);
//    }

    public static void runExplanationTest(String test, String explanation) {
        if (test.equals("transitive")) {
            if (explanation.equals("trace-based")) { // RUNS
                runTraceBasedExplanationTest();
            } else if (explanation.equals("contextual")) { // RUNS
                runContextualExplanationTest();
            } else {
                System.out.println("Invalid explanation: " + explanation);
            }
        } else if (test.equals("food-recommendation")) {
            if (explanation.equals("trace-based")) { // RUNS
                runTraceBasedExplanationTestFoodRecommendation();
            } else if (explanation.equals("contextual")) {
                runContextualExplanationTestFoodRecommendation();
            } else if (explanation.equals("counterfactual")) { 
                // runCounterfactualExplanationTestFoodRecommendation();
            } else if (explanation.equals("contrastive")) { 
                runContrastiveExplanationTestFoodRecommendation();
            } else {
                System.out.println("Invalid explanation: " + explanation);
            }
        } else if (test.equals("loan-eligibility")) {
            if (explanation.equals("trace-based")) { // RUNS
                runTraceBasedExplanationTestLoanEligibility();
            } else if (explanation.equals("contextual")) {
                runContextualExplanationTestLoanEligibility();
            } else if (explanation.equals("counterfactual")) {
                // runCounterfactualExplanationTestLoanEligibility();
            } else if (explanation.equals("contrastive")) {
                runContrastiveExplanationTestLoanEligibility();
            } else {
                System.out.println("Invalid explanation: " + explanation);
            }
        } else {
            System.out.println("Invalid test: " + test);
        }
    }


    public static void run () {
        // Create model...
        PrintUtil.registerPrefix("ex", ModelFactory.getGlobalURI());

        // Trace-Based Explanations
        runExplanationTest("transitive", "trace-based");
        runExplanationTest("food-recommendation", "trace-based");
        runExplanationTest("loan-eligibility", "trace-based");

        // Contextual Explanations
        runExplanationTest("transitive", "contextual");
        runExplanationTest("food-recommendation", "contextual");
        runExplanationTest("loan-eligibility", "contextual");

        // Contrastive Explanations
        runExplanationTest("food-recommendation", "contrastive");
        runExplanationTest("loan-eligibility", "contrastive");
        
        // Counterfactual Explanations
        // runExplanationTest("transitive", "counterfactual"); // FAILS
        runExplanationTest("food-recommendation", "counterfactual");
        runExplanationTest("loan-eligibility", "counterfactual");
        // runCounterfactualExplanationTest();
        // runCounterfactualExplanationTestFoodRecommendation();
        // runCounterfactualExplanationTestLoanEligibility();

        
    }
}