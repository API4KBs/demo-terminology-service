/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omg.demo.terms.components;

import edu.mayo.kmdp.inference.v3.server.QueryApiInternal._askQuery;
import edu.mayo.kmdp.util.JenaUtil;
import edu.mayo.kmdp.util.Util;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.datatypes.Bindings;
import org.omg.spec.api4kp._1_0.services.KnowledgeBase;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;

@Named
public class JenaQuery implements _askQuery {

  @Override
  public Answer<List<Bindings>> askQuery(UUID modelId, String versionTag, KnowledgeCarrier query) {
    return null;
  }

  public Answer<List<Bindings>> askQuery(UUID lambdaId, KnowledgeBase kBase,
      KnowledgeCarrier query) {
    if (isLocal(kBase)) {
      return Answer.of(kBase)
          .map(KnowledgeBase::getManifestation)
          .flatOpt(m -> m.asParseTree(Model.class))
          .flatOpt(m -> applyQuery(query, m));
    } else {
      return askQueryRemote(kBase.getEndpoint().toString(), query);
    }
  }

  private boolean isLocal(KnowledgeBase kBase) {
    return kBase.getEndpoint() == null
        || Util.isEmpty(kBase.getEndpoint().getScheme());
  }

  private Answer<List<Bindings>> askQueryRemote(String endpoint, KnowledgeCarrier query) {
    return Answer.of(
        query.asParseTree(Query.class)
            .map(q -> submitQuery(q, endpoint)));
  }

  private List<Bindings> submitQuery(Query q, String endpoint) {
    Function<RDFNode, String> mapper = (RDFNode::toString);
    QueryExecution queryExec = QueryExecutionFactory.sparqlService(endpoint, q);
    ResultSet results = queryExec.execSelect();
    List<Bindings> answers = new LinkedList<>();
    while (results.hasNext()) {
      QuerySolution sol = results.next();
      Bindings bindings = new Bindings();
      results.getResultVars()
          .forEach(var -> bindings.put(var, mapper.apply(sol.get(var))));
      answers.add(bindings);
    }
    return answers;
  }


  private Optional<List<Bindings>> applyQuery(KnowledgeCarrier query, Model m) {
    return query.asParseTree(Query.class)
        .map(q -> JenaUtil.askQueryResults(m, q))
        .map(this::toBindings);
  }

  private List<Bindings> toBindings(Set<Map<String, String>> binds) {
    return binds.stream()
        .map(m -> {
          Bindings b = new Bindings();
          b.putAll(m);
          return b;
        }).collect(Collectors.toList());
  }

}
