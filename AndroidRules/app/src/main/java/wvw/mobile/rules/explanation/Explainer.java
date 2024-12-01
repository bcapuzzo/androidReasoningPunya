package wvw.mobile.rules.explanation;

// Java Standard Libraries
import java.util.Iterator;
import java.util.List;

// Apache Jena Core Libraries
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

// Apache Jena Reasoner Libraries
import com.hp.hpl.jena.reasoner.Derivation;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.reasoner.rulesys.RuleDerivation;
import com.hp.hpl.jena.util.PrintUtil;

// Android Libraries
import android.util.Log;


/**
 * The <code>Explainer</code> component produces user-friendly
 * explanations of recommendations derived from the <code>Reasoner</code>
 */
/*
@DesignerComponent(version = PunyaVersion.EXPLAINER_COMPONENT_VERSION,
    nonVisible = true,
    category = ComponentCategory.LINKEDDATA,
)
@SimpleObject
 */
public class Explainer {

    // ------------------------------------------------------------
    // Properties
    // ------------------------------------------------------------

    // The base knowledge-graph created by the user. Prior reasoning is not required.
    private Model baseModel;

    // The rules used by the reasoner to make additional assertions on the baseModel.
    private String rules;

    /**
     * Creates a new Explainer component.
     */
    public Explainer(){}

    public Model Model(){
        return this.baseModel;
    }

    public void Model(Model model) {
        this.baseModel = model;
    }

    public String Rules(){
        return this.rules;
    }

    public void Rules(String rules){
        this.rules = rules;
    }

    // ------------------------------------------------------------
    // Logging
    // ------------------------------------------------------------
    

    private static final String TAG = "Explanation-Runner";
    
    // Helper method for consistent logging
    private static void logSection(String title) {
        Log.d(TAG, "\n" + "=".repeat(75));
        Log.d(TAG, title);
        Log.d(TAG, "=".repeat(75));
    }
    
    private static void logSubSection(String title) {
        Log.d(TAG, "\n" + "-".repeat(50));
        Log.d(TAG, title);
        Log.d(TAG, "-".repeat(50));
    }
    
    private static void logDetail(String message) {
        for (String line : message.split("\n")) {
            Log.d(TAG, "\t" + line);
        }
    }

    public static void print(String message) {
        Log.d("Explanatio-Runner", message);
    }



    // ------------------------------------------------------------
    // Trace-Based Explanations
    // ------------------------------------------------------------

    /**
     * Produces a single-sentence contextual explanation as to how the inputted statement
     * was derived by a reasoner.
     * @param subject: The statement's subject. Must be a Resource, or null as a wildcard.
     * @param property: The statement's property. Must be a Property, or null as a wildcard.
     * @param object: The statement's object. Can be a Literal, a Resource, or null as a wildcard
     * @return The traced-base explanation string
     */
    public String GetFullTraceBasedExplanation(Object subject, Object property, Object object) {
        StringBuilder explanation = new StringBuilder("");

        InfModel model = generateInfModel(baseModel);
        logSubSection("Inference Model Contents");
        StmtIterator infStmts = model.listStatements();
        while(infStmts.hasNext()) {
            logDetail(infStmts.next().toString());
        }

        explanation.append(generateTraceBasedExplanation(this.baseModel, model, (Resource)subject,
                (Property) property, (RDFNode) object));

        return explanation.toString();
    }


    // Use the generated inf model, to provide a deep trace for a
    // triple (subject : predicate : object). The base model (containing
    // triples not generated by the reasoner) is needed to check whether
    // a statement was generated by the reasoner or inputted by the user.
    private String generateTraceBasedExplanation(Model baseModel, InfModel inf,
                                                 Resource subject, Property predicate, RDFNode object) {
        logSubSection("Generating Trace-Based Explanation");
        logDetail("Triple: (" + subject + ", " + predicate + ", " + object + ")");

        String answer = "";
        StmtIterator stmtItr = inf.listStatements(subject, predicate, object);

        while (stmtItr.hasNext()) {
            Statement s = stmtItr.next();
            answer += traceDerivation(inf, baseModel, s, 0) + "\n";
            answer += "\n\n";
        }
        return answer;
    }


    // A recursive function that traces through the infModel to determine how the statement was generated
    // by a reasoner, if at all. infModel contains the full RDF model including the triples generated by
    // the reasoner, the baseModel just contains the triples inputted by the user. The statement is the
    // triple that we are tracing back. Tabs specifies the formatting, and can be thought of as the "level"
    // in our model that we're in.
    private String traceDerivation(InfModel infModel, Model baseModel, Statement statement, int tabs) {
        String results = "";

        // Find the triples (matches) and rule that was used to
        // assert this statement, if it exists in the infModel.
        Iterator<Derivation> derivItr = infModel.getDerivation(statement);
        while(derivItr.hasNext()) {
            RuleDerivation derivation = (RuleDerivation) derivItr.next();

            // The concluded triple:
            Triple conclusion = derivation.getConclusion();
            logDetail("Processing conclusion: " + describeTriple(conclusion));
            results += (tabOffset(tabs) + "Conclusion: " + describeTriple(conclusion) + " used the following matches: \n");

            // Goes through the triples that were "matched" with the rule that was fired
            for (Triple match : derivation.getMatches()) {
                Resource matchResource = ResourceFactory.createResource(match.getSubject().getURI());
                Property matchProperty = ResourceFactory.createProperty(match.getPredicate().getURI());
                Node obj = match.getObject();

                if (!obj.isLiteral()) {
                    Resource matchObject = ResourceFactory.createResource(match.getObject().getURI());
                    Statement s = ResourceFactory.createStatement(matchResource, matchProperty, matchObject);

                    if (baseModel.contains(s)) {
                        logDetail("Found user-defined match: " + describeStatement(s));
                        results += tabOffset(tabs) + " Match: " + describeStatement(s) + "\n";
                    }

                    if (!baseModel.contains(s)) {
                        logDetail("Found derived match: " + describeStatement(s));
                        results += tabOffset(tabs) + " Match: " + describeStatement(s) + "\n";
                        results += traceDerivation(infModel, baseModel, s, tabs+1) + "\n";
                    }
                } else {
                    Literal l = ResourceFactory.createTypedLiteral(obj.getLiteralValue().toString(), obj.getLiteralDatatype());
                    Statement s = ResourceFactory.createStatement(matchResource, matchProperty, l);

                    if (baseModel.contains(s)) {
                        logDetail("Found user-defined literal match: " + describeStatement(s));
                        results += tabOffset(tabs) + " Match: " + describeStatement(s) + "\n";
                    }

                    if (!baseModel.contains(s)) {
                        logDetail("Found derived literal match: " + describeStatement(s));
                        results += tabOffset(tabs) + " Match: " + describeStatement(s) + "\n";
                        results += traceDerivation(infModel, baseModel, s, tabs+1) + "\n";
                    }
                }
            }

            logDetail("Applied rule: " + derivation.getRule().toString());
            results += tabOffset(tabs) + "And paired them with the following rule: \n";
            results += tabOffset(tabs) + derivation.getRule().toString() + "\n";
            results += tabOffset(tabs) + "to reach this conclusion.\n";
        }
        return results;
    }



    // ------------------------------------------------------------
    // Contextual Explanations
    // ------------------------------------------------------------

    /**
     * Produces a brief user-readable contextual explanation of how the inputted statement was
     * concluded. Based on the Contextual Ontology:
     * https://tetherless-world.github.io/explanation-ontology/modeling/#casebased/
     * @param resource The resource of the statement.
     * @param property The property of the statement.
     * @param object The object of the statement.
     * @return a shallow trace through the derivations of a statement,
     * formatted in a contextual explanation.
     */
    public String GetShallowContextualExplanation(Object resource, Object property, Object object) {
        logSubSection("Generating Shallow Contextual Explanation");
        logDetail("Triple: (" + resource + ", " + property + ", " + object + ")");

        StringBuilder explanation = new StringBuilder("");

        // Get the derivations produced by the reasoner.
        InfModel model = generateInfModel(baseModel);
        logDetail("Generated Inference Model: " + model);
        
        StmtIterator itr = model
                .listStatements((Resource)resource, (Property) property, (RDFNode) object);

        // Append all explanations to the results.
        while(itr.hasNext()){
            explanation.append(generateShallowTrace(itr.next(), model));
        }

        return explanation.toString();
    }


    /**
     * Generates a shallow Contextual explanation.
     * @param s The statement being derived
     * @param model The InfModel containing the user-set and reasoner-derived knowledge graph.
     * @return A shallow contextual explanation.
     */
    private String generateShallowTrace(Statement s, InfModel model){
        StringBuilder explanation = new StringBuilder("(");

        Iterator<Derivation> itr = model.getDerivation(s);

        while(itr.hasNext()){
            RuleDerivation derivation = (RuleDerivation) itr.next();
            explanation.append(derivation.getConclusion().toString());
            explanation.append("\n");
            explanation.append("( is based on rule ");
            // Print the rule name:
            explanation.append(derivation.getRule().toShortString());
            explanation.append("\n");

            explanation.append("and is in relation to the following situation: \n");
            for (Triple match : derivation.getMatches()){
                Statement binding = generateStatement(match);
                explanation.append(binding.toString());
                explanation.append("\n");

            }

        }

        explanation.append(")");
        return explanation.toString();
    }


    /**
     * Produces a single-sentence contextual explanation as to how the inputted statement
     * was derived by a reasoner.
     * @param resource The resource of the statement.
     * @param property The property of the statement.
     * @param object The object of the statement.
     * @return
     */
    public String GetSimpleContextualExplanation(Object resource, Object property, Object object) {
        logSubSection("Generating Simple Contextual Explanation");
        logDetail("Triple: (" + resource + ", " + property + ", " + object + ")");

        StringBuilder explanation = new StringBuilder("");

        InfModel model = generateInfModel(baseModel);
        logDetail("Generated Inference Model: " + model);

        StmtIterator itr = model
                .listStatements((Resource)resource, (Property) property, (RDFNode) object);

        while(itr.hasNext()) {
            explanation.append(generateSimpleContextualExplanation(itr.next(), model));
            explanation.append("\n\n");
        }
        return explanation.toString();
    }


    /**
     * Generates a simple contextual explanation for a statement, given the
     * model containing the derivations.
     * @param s
     * @param model
     * @return
     */
    private String generateSimpleContextualExplanation(Statement s, InfModel model){
        StringBuilder explanation = new StringBuilder("");

        Iterator<Derivation> itr = model.getDerivation(s);

        while(itr.hasNext()){
            RuleDerivation derivation = (RuleDerivation) itr.next();
            explanation.append(derivation.getConclusion().toString());
            explanation.append(" because ");

            List<Triple> matches = derivation.getMatches();
            int matchIndex = 0;
            for (Triple match : matches){
                Statement binding = generateStatement(match);
                explanation.append(binding.getSubject().toString());
                explanation.append(" ");
                explanation.append(binding.getPredicate().toString());
                explanation.append( " ");
                explanation.append(binding.getObject().toString());
                if (matchIndex < matches.size()-1){
                    explanation.append(", ");
                }

                matchIndex++;
            }

        }
        explanation.append(".");
        return explanation.toString();
    }



    // ------------------------------------------------------------
    // Contrastive Explanations
    // ------------------------------------------------------------

    /**
     * Generate counterfactual explanation for statement by comparing how this.baseModel reached the conclusion
     * compared to how otherBaseModel differs (or match) the conclusion by using the same ruleSet, this.rules.
     * Highlight the difference
     * @param statement the statement (conclusion) to generate explanation
     * @param otherBaseModel the other baseModel to compare this.baseModel to after apply this.rule to both
     * @return The InfModel derived from the reasoner.
     */
    public String GetFullContrastiveExplanation_B(Statement statement, Model otherBaseModel){
        logDetail("Starting contrastive explanation generation");
        logDetail("Analyzing statement: " + statement.toString());
        
        InfModel thisInfModel = generateInfModel(baseModel);
        InfModel otherInfModel = generateInfModel(otherBaseModel);
        logDetail("Generated inference models for comparison");
        
        String results = "";
        StmtIterator itr = thisInfModel.listStatements(statement.getSubject(), statement.getPredicate(), (RDFNode) null);
        StmtIterator itr2 = otherInfModel.listStatements(statement.getSubject(), statement.getPredicate(), (RDFNode) null);
        
        // Find the triples (matches) and rule that was used to assert this statement, if it exists in the infModel.
        Iterator<Derivation> thisDerivItr = thisInfModel.getDerivation(statement);
        Iterator<Derivation> otherDerivItr = otherInfModel.getDerivation(statement);
        
        logDetail("Analyzing derivations for both models...");
        
        while (thisDerivItr.hasNext()) {
            // This model derivation
            RuleDerivation thisDerivation = (RuleDerivation) thisDerivItr.next();
            RuleDerivation otherDerivation = null;
            
            logDetail("Processing current model derivation");
            logDetail("Current model rule: " + thisDerivation.getRule().toString());

            // Complete derivation match
            if (otherDerivItr.hasNext()) {
                otherDerivation = (RuleDerivation) otherDerivItr.next();
                logDetail("Found matching derivation in alternate model");
            }
            // Partial derivation match (same subject and predicate, but different object)
            else if (itr2.hasNext()) {
                Statement otherMatch = itr2.next();
                logDetail("Found partial match in alternate model: " + otherMatch.toString());
                otherDerivItr = otherInfModel.getDerivation(otherMatch);
                otherDerivation = (RuleDerivation) otherDerivItr.next();
            } else {
                logDetail("No matching derivation found in alternate model");
            }
            
            Triple thisConclusion = thisDerivation.getConclusion();
            Triple otherConclusion = null;
            if (otherDerivation != null) {
                otherConclusion = otherDerivation.getConclusion();
                logDetail("Comparing conclusions:");
                logDetail("This model: " + describeTriple(thisConclusion));
                logDetail("Other model: " + describeTriple(otherConclusion));
            }

            if (otherConclusion == null) {
                logDetail("No conclusion found in alternate model");
                results += "This model concluded: " + describeTriple(thisConclusion) + "\n";
                results += "Alternate model didn't conclude anything.\n";
                return results;
            } else if (thisConclusion.sameAs(otherConclusion.getSubject(),
                    otherConclusion.getPredicate(),
                    otherConclusion.getObject())) {
                logDetail("Both models reached the same conclusion");
                results += "Both model concluded: " + describeTriple(thisConclusion) + "\n";
                logDetail("Recursively analyzing matches...");
                for (Triple match : thisDerivation.getMatches()) {
                    Statement matchStatement = generateStatement(match);
                    results += GetFullContrastiveExplanation_B(matchStatement, otherBaseModel) + "\n";
                }
            } else {
                logDetail("Models reached different conclusions");
                results += "This model concluded: " + describeTriple(thisConclusion) + " using Matches: \n";
                
                logDetail("Analyzing matches from current model:");
                for (Triple match : thisDerivation.getMatches()) {
                    Statement matchStatement = generateStatement(match);
                    logDetail("Match: " + describeStatement(matchStatement));
                    results +=  "  " + describeStatement(matchStatement) + "\n";
                }
                
                logDetail("Analyzing matches from alternate model:");
                results += "Alternate model concluded: " + describeTriple(otherConclusion) + " instead using Matches: \n";
                for (Triple match2 : otherDerivation.getMatches()) {
                    Statement matchStatement = generateStatement(match2);
                    logDetail("Match: " + describeStatement(matchStatement));
                    results += "  " + describeStatement(matchStatement) + "\n";
                }
                
                logDetail("Recursively analyzing derived matches...");
                for (Triple match : thisDerivation.getMatches()) {
                    Statement matchStatement = generateStatement(match);
                    if (!baseModel.contains(matchStatement)) {
                        logDetail("Analyzing derived match: " + describeStatement(matchStatement));
                        results += GetFullContrastiveExplanation_B(matchStatement, otherBaseModel) + "\n";
                    }
                }
            }
        }
        return results;
    }


    // Helper method to describe a triple
    private String describeTriple(Triple triple) {
        // String subject = triple.getSubject().toString();
        String subjectURI = triple.getSubject().getURI();
        String[] subjectParts = subjectURI.split("/");
        String subject = subjectParts[subjectParts.length - 1];

        // String predicate = triple.getPredicate().toString();
        String predicateURI = triple.getPredicate().getURI();
        String[] predicateParts = predicateURI.split("/");
        String predicate = predicateParts[predicateParts.length - 1];

        String object;
        if (triple.getObject().isLiteral()) {
            String literalValue = triple.getObject().getLiteral().toString();
            String[] objectParts = literalValue.split("\\^\\^");
            // Return the first part which contains the numeric value
            object = objectParts[0];
        } else {
            // object = triple.getObject().toString();
            String objectURI = triple.getObject().getURI();
            String[] objectParts = objectURI.split("/");
            object = objectParts[objectParts.length - 1];
        }
        return "Subject: " + subject + " , Predicate: " + predicate + ", Object: " + object;
    }


    // Helper method to describe a statement
    private String describeStatement(Statement statement) {
        String subjectURI = statement.getSubject().getURI();
        String[] subjectParts = subjectURI.split("/");
        String subject = subjectParts[subjectParts.length - 1];

        String predicateURI = statement.getPredicate().getURI();
        String[] predicateParts = predicateURI.split("/");
        String predicate = predicateParts[predicateParts.length - 1];

        String object;
        String literalValue;
        RDFNode objectNode = statement.getObject();
        if (objectNode.isLiteral()) {
            literalValue = objectNode.toString();
            String[] objectParts = literalValue.split("\\^\\^");
            // Return the first part which contains the numeric value
            object = objectParts[0];
        } else if (objectNode instanceof Resource) {
            Resource resource = (Resource) objectNode;
            if (resource.isURIResource()) {
                String objectURI = resource.getURI();
                String[] objectParts = objectURI.split("/");
                object = objectParts[objectParts.length - 1];
            } else {
                // Handle blank nodes or other resource types as needed
                object = resource.toString();
            }
        } else {
            // Handle other types of nodes if necessary
            object = objectNode.toString();
        }
        return "Subject: " + subject + ", Predicate: " + predicate + ", Object: " + object;
    }
  


    // ------------------------------------------------------------
    // Counterfactual Explanation
    // ------------------------------------------------------------

    /**
     * Generates a counterfactual explanation by comparing an instance to other instances
     * with a desired outcome.
     * 
     * @param targetInstance The instance to generate explanation for
     * @param outcomeProperty The property indicating the outcome (e.g., eligibility)
     * @param feature1Property First feature to compare (e.g., credit score)
     * @param feature2Property Second feature to compare (e.g., DTI ratio)
     * @return A string explanation of what changes are needed
     */
    public String GetCounterfactualExplanation(Statement statement) {
        InfModel infModel = generateInfModel(baseModel);
        StringBuilder explanation = new StringBuilder();
        
        Resource targetInstance = statement.getSubject();
        Property outcomeProperty = statement.getPredicate();
        RDFNode currentOutcome = statement.getObject();
        
        Statement outcome = getStatement(infModel, targetInstance, outcomeProperty);
        if (outcome == null) {
            logDetail("Error: Could not find outcome property for target instance");
            return "Error: Could not find outcome property for target instance";
        }
        
        explanation.append("To change the outcome for ").append(targetInstance.getLocalName())
                  .append(" from '").append(outcome.getObject().toString())
                  .append("', you could look at these examples:\n\n");
        
        // Find instances with different outcomes
        StmtIterator otherInstances = infModel.listStatements(null, outcomeProperty, (RDFNode) null);
        
        while (otherInstances.hasNext()) {
            Statement otherStatement = otherInstances.next();
            Resource otherInstance = otherStatement.getSubject();
            
            // Skip if it's the target instance
            if (otherInstance.equals(targetInstance)) continue;
            
            // If this instance has a different outcome, analyze why
            if (!otherStatement.getObject().equals(currentOutcome)) {
                explanation.append(otherInstance.getLocalName())
                          .append(" is '").append(otherStatement.getObject().toString())
                          .append("' because:\n");
                
                // List properties that are different
                StmtIterator properties = infModel.listStatements(targetInstance, null, (RDFNode) null);
                while (properties.hasNext()) {
                    Statement prop = properties.next();
                    Statement otherProp = getStatement(infModel, otherInstance, prop.getPredicate());
                    
                    // Skip certain properties and those with same values
                    if (otherProp != null && 
                        !prop.getPredicate().equals(outcomeProperty) &&
                        !prop.getObject().equals(otherProp.getObject()) &&
                        !prop.getPredicate().getLocalName().equals("type") &&
                        !prop.getPredicate().getLocalName().equals("name")) {
                        
                        String propName = formatPropertyName(prop.getPredicate().getLocalName());
                        String currentValue = cleanValue(prop.getObject().toString());
                        String otherValue = cleanValue(otherProp.getObject().toString());
                        
                        explanation.append("- Their ").append(propName)
                                  .append(" is ").append(otherValue)
                                  .append(" while yours is ").append(currentValue)
                                  .append("\n");
                    }
                }
                explanation.append("\n");
            }
        }
        
        return explanation.toString();
    }

    private String cleanValue(String value) {
        // Remove data type annotations from values
        if (value.contains("^^")) {
            return value.substring(0, value.indexOf("^^"));
        }
        return value;
    }



    // ------------------------------------------------------------
    // Utility Methods
    // ------------------------------------------------------------

    /**
     * Runs a reasoner on the Linked Data. Guarantees derivations are
     * stored.
     * @return The InfModel derived from the reasoner.
     */
    private InfModel generateInfModel(Model baseModel){
        // Register prefixes before creating the reasoner
        PrintUtil.registerPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        PrintUtil.registerPrefix("schema", "http://schema.org/");
        PrintUtil.registerPrefix("ex", "http://example.com/");
        PrintUtil.registerPrefix("foaf", "http://xmlns.com/foaf/0.1/");
        PrintUtil.registerPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
        
        Reasoner reasoner = new GenericRuleReasoner(Rule.parseRules(rules));
        reasoner.setDerivationLogging(true);
        return com.hp.hpl.jena.rdf.model.ModelFactory.createInfModel(reasoner, baseModel);
    }

    /**
     * Generates a statement using the URIS present in the triple.
     * @param triple
     * @return A basic statement
     */
    private Statement generateStatement(Triple triple){
        Resource subject = ResourceFactory.createResource(triple.getSubject().getURI());
        Property property = ResourceFactory.createProperty(triple.getPredicate().getURI());
        Node obj = triple.getObject();
        if (obj.isLiteral()){
            Literal l = ResourceFactory.createTypedLiteral(obj.getLiteralValue().toString(), obj.getLiteralDatatype());
            return ResourceFactory.createStatement(subject, property, l);
        }
        if (!obj.isLiteral()){
            Resource matchObject = ResourceFactory.createResource(triple.getObject().getURI());
            return ResourceFactory.createStatement(subject, property, matchObject);
        }
        // Should never reach here.
        return null;
    }

    private Statement getStatement(InfModel model, Resource subject, Property predicate) {
        StmtIterator itr = model.listStatements(subject, predicate, (RDFNode)null);
        if (itr.hasNext()) {
            return itr.next();
        }
        return null;
    }

    // returns a string with @num tabs in it
    private String tabOffset(int num) {
        String tab = "";
        for (int i=0; i < num; i++) {
            tab += ("\t");
        }
        return tab;
    }

    /**
     * Formats a property name from snake_case to Title Case
     * e.g., "credit_score" -> "Credit Score"
     */
    private String formatPropertyName(String propertyName) {
        String[] words = propertyName.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                // Special case for abbreviations like DTI
                if (word.length() <= 3) {
                    formatted.append(word.toUpperCase());
                } else {
                    formatted.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1).toLowerCase());
                }
                formatted.append(" ");
            }
        }
        return formatted.toString().trim();
    }

}
