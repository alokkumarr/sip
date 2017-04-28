import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods.parse
import org.scalatest.FunSpec
import org.scalatest.MustMatchers

import model.QueryBuilder

class QueryBuilderTest extends FunSpec with MustMatchers {
  describe("Query built from analysis") {
    it("should have SELECT and FROM") {
      query(artifactT) must be ("SELECT t.a, t.b FROM t")
    }
    it("with filters should have a WHERE clause") {
      query(artifactT, filters(
        filter("", "t.a", ">", "1"),
        filter("AND", "t.b", "<", "2"))
      ) must be ("SELECT t.a, t.b FROM t WHERE t.a > 1 AND t.b < 2")
    }
    it("with group by columns should have an GROUP BY clause") {
      query(artifactT, groupBy("t.a", "t.b")
      ) must be ("SELECT t.a, t.b FROM t GROUP BY t.a, t.b")
    }
    it("with order by columns should have an ORDER BY clause") {
      query(artifactT, orderBy("t.a ASC", "t.b DESC")
      ) must be ("SELECT t.a, t.b FROM t ORDER BY t.a ASC, t.b DESC")
    }
  }

  describe("Query built from analysis with multiple artifacts") {
    it("should list all columns in SELECT and all tables in FROM clause") {
      queryTwo(artifactT)(artifactU) must be (
        "SELECT t.a, t.b, u.c, u.d FROM t, u")
    }
  }

  private def artifactT = {
    artifact("t", "a", "b")
  }

  private def artifactU = {
    artifact("u", "c", "d")
  }

  private def query(objs: JObject*): String = {
    val artifact = objs.reduceLeft(_ merge _)
    val analysis = ("artifacts", List(artifact))
    QueryBuilder.build(analysis)
  }

  private def queryTwo(objs1: JObject*)(objs2: JObject*): String = {
    val artifact1 = objs1.reduceLeft(_ merge _)
    val artifact2 = objs2.reduceLeft(_ merge _)
    val analysis = ("artifacts", List(artifact1, artifact2))
    QueryBuilder.build(analysis)
  }

  private def artifact(name: String, columns: String*): JObject = {
    ("artifactName", name) ~
    ("columns", columns.map(("columnName", _)))
  }

  private def filters(filters: JObject*): JObject = {
    ("filters", filters.toList)
  }

  private def filter(bool: String, name: String, operator: String,
    cond: String): JObject = {
    ("booleanCriteria", bool) ~
    ("columnName", name) ~
    ("operator", operator) ~
    ("searchConditions", cond)
  }

  private def orderBy(name: String*) = {
    ("orderByColumns", name)
  }

  private def groupBy(name: String*) = {
    ("groupByColumns", name)
  }
}
