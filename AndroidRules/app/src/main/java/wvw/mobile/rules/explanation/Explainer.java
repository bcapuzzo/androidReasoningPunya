package wvw.mobile.rules.explanation;


import android.util.Log;

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
import com.hp.hpl.jena.reasoner.Derivation;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.reasoner.rulesys.RuleDerivation;

import java.util.Iterator;
import java.util.List;


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

    // The base knowledge-graph created by the user. Prior reasoning
    // is not required.
    private Model baseModel;

    // The rules used by the reasoner to make additional assertions on
    // the baseModel.
    private String rules;

    /**
     * Creates a new Explainer component.
     */
    public Explainer(){}

    /// region Properties

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

    ///endregion
    ///region Methods

    /**
     * Generate counterfactual explanation for statement by comparing how this.baseModel reached the conclusion
     * compared to how otherBaseModel differs(or match) the conclusion by using the same ruleSet, this.rules.
     * Highlight the difference
     * @param statement the statement (conclusion) to generate explanation
     * @param otherBaseModel the other baseModel to compare this.baseModel to after apply this.rule to both
     * @return The InfModel derived from the reasoner.
     */
    public String GetFullCounterfactualExplanation(Statement statement, Model otherBaseModel){
        InfModel thisInfModel = generateInfModel(baseModel);
        InfModel otherInfModel = generateInfModel(otherBaseModel);
        String results = "";
        StmtIterator itr = thisInfModel.listStatements(statement.getSubject(), statement.getPredicate(), (RDFNode) null);
        StmtIterator itr2 = otherInfModel.listStatements(statement.getSubject(), statement.getPredicate(), (RDFNode) null);

        // Find the triples (matches) and rule that was used to
        // assert this statement, if it exists in the infModel.
        Iterator<Derivation> thisDerivItr = thisInfModel.getDerivation(statement);
        Iterator<Derivation> otherDerivItr = otherInfModel.getDerivation(statement);

        while (thisDerivItr.hasNext()) {
            // This model derivation
            RuleDerivation thisDerivation = (RuleDerivation) thisDerivItr.next();
            RuleDerivation otherDerivation = null;

            // Complete derivation match
            if (otherDerivItr.hasNext()) {
                otherDerivation = (RuleDerivation) otherDerivItr.next();
            }
            // Partial derivation match (same subject, predicate but different object
            else if (itr2.hasNext()) {
                Statement otherMatch = itr2.next();
                otherDerivItr = otherInfModel.getDerivation(otherMatch);
                otherDerivation = (RuleDerivation) otherDerivItr.next();
            }
            Triple thisConclusion = thisDerivation.getConclusion();
            Triple otherConclusion = null;
            if (otherDerivation != null)
                otherConclusion = otherDerivation.getConclusion();

            if (otherConclusion == null) {
                results += "This model concluded: " + thisConclusion.toString() + "\n";
                results += "Alternate model didn't conclude anything.\n";
                System.out.print(results);
                return results;
            } else if (thisConclusion.sameAs(otherConclusion.getSubject(),
                    otherConclusion.getPredicate(),
                    otherConclusion.getObject())) {
                results += "Both model concluded: " + thisConclusion.toString() + "\n";
                for (Triple match : thisDerivation.getMatches()) {
                    Statement matchStatement = generateStatement(match);
                    results += GetFullCounterfactualExplanation(matchStatement, otherBaseModel) + "\n";
                }
            } else {
                results += "This model concluded: " + thisConclusion.toString() + " using Matches: \n";
                for (Triple match : thisDerivation.getMatches()) {
                    Statement matchStatement = generateStatement(match);
                    results +=  " Match: " + matchStatement.toString() + "\n";
                }
                results += "Alternate model concluded: " + otherConclusion.toString() + " instead using Matches: \n";
                for (Triple match2 : otherDerivation.getMatches()) {
                    Statement matchStatement = generateStatement(match2);
                    results += " Match: " + matchStatement.toString() + "\n";
                }
                // Recurse
                for (Triple match : thisDerivation.getMatches()) {
                    Statement matchStatement = generateStatement(match);
                    if (!baseModel.contains(matchStatement)) {
                        results += GetFullCounterfactualExplanation(matchStatement, otherBaseModel) + "\n";
                    }
                }
            }
        }
        return results;
    }


    // ==================================================================================
    // ==========================     capuzb Contributions     ==========================
    // ==================================================================================

    /**
     * Generate counterfactual explanation for statement by comparing how this.baseModel reached the conclusion
     * compared to how otherBaseModel differs (or match) the conclusion by using the same ruleSet, this.rules.
     * Highlight the difference
     * @param statement the statement (conclusion) to generate explanation
     * @param otherBaseModel the other baseModel to compare this.baseModel to after apply this.rule to both
     * @return The InfModel derived from the reasoner.
     */
    public String GetFullCounterfactualExplanation_B(Statement statement, Model otherBaseModel){
        InfModel thisInfModel = generateInfModel(baseModel);
        InfModel otherInfModel = generateInfModel(otherBaseModel);
        String results = "";
        StmtIterator itr = thisInfModel.listStatements(statement.getSubject(), statement.getPredicate(), (RDFNode) null);
        StmtIterator itr2 = otherInfModel.listStatements(statement.getSubject(), statement.getPredicate(), (RDFNode) null);

        // Find the triples (matches) and rule that was used to assert this statement, if it exists in the infModel.
        Iterator<Derivation> thisDerivItr = thisInfModel.getDerivation(statement);
        Iterator<Derivation> otherDerivItr = otherInfModel.getDerivation(statement);

        while (thisDerivItr.hasNext()) {
            // This model derivation
            RuleDerivation thisDerivation = (RuleDerivation) thisDerivItr.next();
            RuleDerivation otherDerivation = null;

            // Complete derivation match
            if (otherDerivItr.hasNext()) {
                otherDerivation = (RuleDerivation) otherDerivItr.next();
            }
            // Partial derivation match (same subject and predicate, but different object)
            else if (itr2.hasNext()) {
                Statement otherMatch = itr2.next();
                otherDerivItr = otherInfModel.getDerivation(otherMatch);
                otherDerivation = (RuleDerivation) otherDerivItr.next();
            }
            Triple thisConclusion = thisDerivation.getConclusion();
            Triple otherConclusion = null;
            if (otherDerivation != null)
                otherConclusion = otherDerivation.getConclusion();

            if (otherConclusion == null) {
                results += "This model concluded: " + describeTriple(thisConclusion) + "\n";
                results += "Alternate model didn't conclude anything.\n";
                System.out.print(results);
                return results;
            } else if (thisConclusion.sameAs(otherConclusion.getSubject(),
                    otherConclusion.getPredicate(),
                    otherConclusion.getObject())) {
                results += "Both model concluded: " + describeTriple(thisConclusion) + "\n";
                for (Triple match : thisDerivation.getMatches()) {
                    Statement matchStatement = generateStatement(match);
                    results += GetFullCounterfactualExplanation_B(matchStatement, otherBaseModel) + "\n";
                }
            } else {
                results += "This model concluded: " + describeTriple(thisConclusion) + " using Matches: \n";
                for (Triple match : thisDerivation.getMatches()) {
                    Statement matchStatement = generateStatement(match);
                    // results +=  " Match: " + describeStatement(matchStatement) + "\n";
                    results +=  "  " + describeStatement(matchStatement) + "\n";
                }
                results += "Alternate model concluded: " + describeTriple(otherConclusion) + " instead using Matches: \n";
                for (Triple match2 : otherDerivation.getMatches()) {
                    Statement matchStatement = generateStatement(match2);
                    // results += " Match: " + describeStatement(matchStatement) + "\n";
                    results += "  " + describeStatement(matchStatement) + "\n";
                }
                // Recurse
                for (Triple match : thisDerivation.getMatches()) {
                    Statement matchStatement = generateStatement(match);
                    if (!baseModel.contains(matchStatement)) {
                        results += GetFullCounterfactualExplanation_B(matchStatement, otherBaseModel) + "\n";
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


    /**
     * Produces a single-sentence contextual explanation as to how the inputted statement
     * was derived by a reasoner.
     * @param subject: The statement's subject. Must be a Resource, or null as a wildcard.
     * @param property: The statement's property. Must be a Property, or null as a wildcard.
     * @param object: The statement's object. Can be a Literal, a Resource, or null as a wildcard
     * @return The traced-base explanation string
     */
    public String GetFullTracedBaseExplanation_B(Object subject, Object property, Object object) {
        StringBuilder explanation = new StringBuilder("");

        InfModel model = generateInfModel(baseModel);

        explanation.append(generateTraceBasedExplanation_B(this.baseModel, model, (Resource)subject,
                (Property) property, (RDFNode) object));

        return explanation.toString();
    }


    // Use the generated inf model, to provide a deep trace for a
    // triple (subject : predicate : object). The base model (containing
    // triples not generated by the reasoner) is needed to check whether
    // a statement was generated by the reasoner or inputted by the user.
    private String generateTraceBasedExplanation_B(Model baseModel, InfModel inf,
                                                 Resource subject, Property predicate, RDFNode object) {
        Log.d("Explanation-Runner", "\tGenerating Trace-Based Explanation...\n\t\t(" + subject + ", " + predicate + ", " + object + ")");

        String answer = "";
        StmtIterator stmtItr = inf.listStatements(subject, predicate, object);
        while ( stmtItr.hasNext() )
        {
            Statement s = stmtItr.next();
            answer += traceDerivation_B(inf, baseModel, s, 0) + "\n";
            answer += "\n\n";

        }
        return answer;
    }


    // A recursive function that traces through the infModel to determine how the statement was generated
    // by a reasoner, if at all. infModel contains the full RDF model including the triples generated by
    // the reasoner, the baseModel just contains the triples inputted by the user. The statement is the
    // triple that we are tracing back. Tabs specifies the formatting, and can be thought of as the "level"
    // in our model that we're in.
    private String traceDerivation_B(InfModel infModel, Model baseModel, Statement statement, int tabs) {
        String results = "";

        // Find the triples (matches) and rule that was used to
        // assert this statement, if it exists in the infModel.
        Iterator<Derivation> derivItr = infModel.getDerivation(statement);
        while(derivItr.hasNext()) {

            // Should be the same as statement.
            // Multiple are possible if a wildcard is used within statement.
            RuleDerivation derivation = (RuleDerivation) derivItr.next();

            // The concluded triple:
            Triple conclusion = derivation.getConclusion();
            results += (tabOffset(tabs) + "Conclusion: " + describeTriple(conclusion) + " used the following matches: \n");

            // Goes through the triples that were "matched" with the rule that was fired to
            // generate the concluded triple.
            for (Triple match : derivation.getMatches()) {

                // Use the URIs to generate a statement, so we can search the base model for the statement.
                Resource matchResource = ResourceFactory.createResource(match.getSubject().getURI());
                Property matchProperty = ResourceFactory.createProperty(match.getPredicate().getURI());

                Node obj = match.getObject();

                if (!obj.isLiteral()) {
                    Resource matchObject = ResourceFactory.createResource(match.getObject().getURI());

                    Statement s = ResourceFactory.createStatement(matchResource, matchProperty, matchObject);

                    // If the match was added by the user, we don't need to trace through how the reasoner derived
                    // that statement, since the user added it themselves...
                    if (baseModel.contains(s)) {
                        results += tabOffset(tabs) + " Match: " + describeStatement(s) + "\n";
                    }

                    // Assuming it's not in the base model, then the reasoner must have derived that statement...
                    if (!baseModel.contains(s)) {
                        results += tabOffset(tabs) + " Match: " + describeStatement(s) + "\n";

                        // Recursively trace to find how the reasoner derived that statement.
                        results += traceDerivation_B(infModel, baseModel, s, tabs+1) + "\n";
                    }
                } else {
                    Literal l = ResourceFactory.createTypedLiteral(obj.getLiteralValue().toString(), obj.getLiteralDatatype());
                    Statement s = ResourceFactory.createStatement(matchResource, matchProperty, l);

                    if (baseModel.contains(s)) {
                        results += tabOffset(tabs) + " Match: " + describeStatement(s) + "\n";
                    }

                    // Assuming it's not in the base model, then the reasoner must have derived that statement...
                    if (!baseModel.contains(s)) {
                        results += tabOffset(tabs) + " Match: " + describeStatement(s) + "\n";

                        // Recursively trace to find how the reasoner derived that statement.
                        results += traceDerivation_B(infModel, baseModel, s, tabs+1) + "\n";
                    }
                }
            }

            // After going through the matches, we now print the rule that the matches matched to.
            results += tabOffset(tabs) + "And paired them with the following rule: \n" ;
            results += tabOffset(tabs) + derivation.getRule().toString() + "\n";
            results += tabOffset(tabs) + "to reach this conclusion.\n";

        }
        return results;
    }


    // ==================================================================================
    // ==========================     End of Contributions     ==========================
    // ==================================================================================




    /**
     * Produces a single-sentence contextual explanation as to how the inputted statement
     * was derived by a reasoner.
     * @param subject: The statement's subject. Must be a Resource, or null as a wildcard.
     * @param property: The statement's property. Must be a Property, or null as a wildcard.
     * @param object: The statement's object. Can be a Literal, a Resource, or null as a wildcard
     * @return The traced-base explanation string
     */
    public String GetFullTracedBaseExplanation(Object subject, Object property, Object object) {
        StringBuilder explanation = new StringBuilder("");

        InfModel model = generateInfModel(baseModel);

        explanation.append(generateTraceBasedExplanation(this.baseModel, model, (Resource)subject,
                (Property) property, (RDFNode) object));

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
    public String GetSimpleContextualExplanation(Object resource, Object property, Object object){
        Log.d("Explanation-Runner", "\tGenerating Simple Contextual Explanation for (" + resource + ", " + property + ", " + object + ")");

        StringBuilder explanation = new StringBuilder("");

        InfModel model = generateInfModel(baseModel);
        Log.d("Explanation-Runner", "\t\tGetSimpleContextualExplanation() Model:\n\t\t\t" + model);

        StmtIterator itr = model
                .listStatements((Resource)resource, (Property) property, (RDFNode) object);

        while(itr.hasNext()){
            explanation.append(generateSimpleContextualExplanation(itr.next(), model));
            explanation.append("\n\n");
        }
        return explanation.toString();
    }

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
        Log.d("Explanation-Runner", "\tGenerating Shallow Contextual Explanation for (" + resource + ", " + property + ", " + object + ")");

        StringBuilder explanation = new StringBuilder("");

        // Get the derivations produced by the reasoner.
        InfModel model = generateInfModel(baseModel);
        Log.d("Explanation-Runner", "\t\tGetShallowContextualExplanation() Model:\n\t\t\t" + model);
        StmtIterator itr = model
                .listStatements((Resource)resource, (Property) property, (RDFNode) object);

        // Append all explanations to the results.
        while(itr.hasNext()){
            explanation.append(generateShallowTrace(itr.next(), model));
        }

        return explanation.toString();
    }
    ///endregion

    ///region Contextual Reasoning Helper Methods

    /**
     * Runs a reasoner on the Linked Data. Guarantees derivations are
     * stored.
     * @return The InfModel derived from the reasoner.
     */
    private InfModel generateInfModel(Model baseModel){
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
    ///endregion

    ///region Trace Explanation Helper Methods

    // returns a string with @num tabs in it
    private String tabOffset(int num) {
        String tab = "";
        for (int i=0; i < num; i++) {
            tab += ("\t");
        }
        return tab;
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

            // Should be the same as statement.
            // Multiple are possible if a wildcard is used within statement.
            RuleDerivation derivation = (RuleDerivation) derivItr.next();

            // The concluded triple:
            Triple conclusion = derivation.getConclusion();
            results += (tabOffset(tabs) + "Conclusion: " + conclusion.toString() + " used the following matches: \n");

            // Goes through the triples that were "matched" with the rule that was fired to
            // generate the concluded triple.
            for (Triple match : derivation.getMatches()) {

                // Use the URIs to generate a statement, so we can search the base model for the statement.
                Resource matchResource = ResourceFactory.createResource(match.getSubject().getURI());
                Property matchProperty = ResourceFactory.createProperty(match.getPredicate().getURI());

                Node obj = match.getObject();

                if (!obj.isLiteral()) {
                    Resource matchObject = ResourceFactory.createResource(match.getObject().getURI());

                    Statement s = ResourceFactory.createStatement(matchResource, matchProperty, matchObject);

                    // If the match was added by the user, we don't need to trace through how the reasoner derived
                    // that statement, since the user added it themselves...
                    if (baseModel.contains(s)) {
                        results += tabOffset(tabs) + " Match: " + s.toString() + " was asserted by the user\n";
                    }

                    // Assuming it's not in the base model, then the reasoner must have derived that statement...
                    if (!baseModel.contains(s)) {
                        results += tabOffset(tabs) + " Match: " + s.toString() + " was asserted by the reasoner.\n";

                        // Recursively trace to find how the reasoner derived that statement.
                        results += traceDerivation(infModel, baseModel, s, tabs+1) + "\n";
                    }
                } else {
                    Literal l = ResourceFactory.createTypedLiteral(obj.getLiteralValue().toString(), obj.getLiteralDatatype());
                    Statement s = ResourceFactory.createStatement(matchResource, matchProperty, l);

                    if (baseModel.contains(s)) {
                        results += tabOffset(tabs) + " Match: " + s.toString() + " was asserted by the user\n";
                    }

                    // Assuming it's not in the base model, then the reasoner must have derived that statement...
                    if (!baseModel.contains(s)) {
                        results += tabOffset(tabs) + " Match: " + s.toString() + " was asserted by the reasoner.\n";

                        // Recursively trace to find how the reasoner derived that statement.
                        results += traceDerivation(infModel, baseModel, s, tabs+1) + "\n";
                    }
                }
            }

            // After going through the matches, we now print the rule that the matches matched to.
            results += tabOffset(tabs) + "And paired them with the following rule: \n" ;
            results += tabOffset(tabs) + derivation.getRule().toString() + "\n";
            results += tabOffset(tabs) + "to reach this conclusion.\n";

        }
        return results;
    }

    // Use the generated inf model, to provide a deep trace for a
    // triple (subject : predicate : object). The base model (containing
    // triples not generated by the reasoner) is needed to check whether
    // a statement was generated by the reasoner or inputted by the user.
    private String generateTraceBasedExplanation(Model baseModel, InfModel inf,
                                                 Resource subject, Property predicate, RDFNode object) {
        Log.d("Explanation-Runner", "Generating Trace-Based Explanation...\n\t(" + subject + ", " + predicate + ", " + object + ")");

        String answer = "";
        StmtIterator stmtItr = inf.listStatements(subject, predicate, object);
        while ( stmtItr.hasNext() )
        {
            Statement s = stmtItr.next();
            answer += traceDerivation(inf, baseModel, s, 0) + "\n";
            answer += "\n\n";

        }
        return answer;
    }
    //endregion

}
