package deductions.runtime.stresstests

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import io.gatling.core.scenario._

class StressSimulation extends Simulation {

val uris = List(
  "http://id.dbpedia.org/resource/GPRS" ,
  "http://www.wikidata.org/entity/statement/Q7397-72B3813E-BCB5-471A-B5C3-47321CF1914D" ,
  "http://fr.wikipedia.org/wiki/DFSG" ,
  "http://rdf.freebase.com/ns/m.02wtqd4" ,
  "http://www.wikidata.org/entity/Q5613113" ,
  "http://www.wikidata.org/entity/Q874405" ,
  "http://rdf.freebase.com/ns/m.02hb4m" ,
  "http://fr.wikipedia.org/wiki/Open_Source" ,
  "http://www.wikidata.org/entity/Q15021033" ,
  "http://www.wikidata.org/entity/Q11241033" ,
  "http://id.dbpedia.org/resource/SCSI" ,
  "http://fr.wikipedia.org/wiki/DFSG?oldid=10190603" ,
  "http://fr.wikipedia.org/wiki/Open_Source?oldid=42687883" ,
  "http://fr.wikipedia.org/wiki/Modèle:Nb_p.?oldid=103354158" ,
  "http://www.wikidata.org/entity/statement/Q7397-1F2024BB-66E1-4B83-ADA5-A2B91D80034D" ,
  "http://www.wikidata.org/entity/statement/Q7397-a7b03d9c-44f2-90ff-1d59-1a228eaf3062" ,
  "http://www.wikidata.org/entity/statement/Q7397-c90bd89e-423d-3bbb-b265-d3dda89d3968" ,
  "http://www.wikidata.org/entity/statement/Q7397-69c0806f-4908-757a-8187-a5e859054c27" ,
  "http://www.wikidata.org/entity/statement/Q7397-5AD7A79D-45D3-4AE5-A2B7-D6EE7C9C20C0" ,
  "http://www.wikidata.org/reference/004ec6fbee857649acdbdbad4f97b2c8571df97b" ,
  "http://www.wikidata.org/entity/statement/Q5613113-3f2ec00f-48d2-6685-b1a0-38a58dd690ad" ,
  "http://www.wikidata.org/entity/statement/q5613113-B5D5AEE2-40B1-4BB1-92E2-97C7A795097E" ,
  "http://www.wikidata.org/entity/statement/q5613113-0BEAA065-ED61-41F8-99AB-A109BD629D99" ,
  "http://www.wikidata.org/entity/statement/Q11241033-0f04c539-436f-d67e-afa3-43666eb8c616" ,
  "http://fr.dbpedia.org/resource/Vrms" ,
  "http://www.wikidata.org/entity/Q151885" ,
  "http://www.wikidata.org/entity/Q7232559" ,
  "http://www.wikidata.org/entity/Q2657718" ,
  "http://www.wikidata.org/entity/Q16334295" ,
  "http://www.wikidata.org/entity/Q4663903" ,
  "http://fr.dbpedia.org/resource/Renommage_des_applications_de_Mozilla_par_Debian" ,
  "http://www.wikidata.org/entity/statement/q874405-58CCE52E-D915-41D4-A9B0-5BEA5DDC869A" ,
  "http://www.wikidata.org/entity/statement/Q874405-7A5AC02E-9E6E-426B-A184-1EDFACE121F9" ,
  "http://www.wikidata.org/entity/statement/Q874405-558e7dfe-428c-2b2d-c354-ac38e49d2bd4" ,
  "http://www.wikidata.org/entity/statement/Q874405-9AB26014-4848-4DF1-940B-D40574BBD69C" ,
  "http://www.wikidata.org/entity/statement/Q874405-0AC5BA79-B484-42D9-A486-AA9AB422CAFB" ,
  "http://www.wikidata.org/entity/statement/Q874405-0D1F7BEB-00BB-4D28-87F3-DA85A73295AA" ,
  "http://www.wikidata.org/entity/statement/Q874405-D25D85A7-F222-4157-A877-5F4B84506861" ,
  "http://www.wikidata.org/entity/statement/Q874405-e6bbc32b-46f3-4a49-e950-bd1162ca4943" ,
  "http://www.wikidata.org/entity/statement/Q874405-d51b7d35-4c08-8ceb-cbeb-c2c08532408d" ,
  "http://www.wikidata.org/reference/7eb64cf9621d34c54fd4bd040ed4b61a88c4a1a0" ,
  "http://www.wikidata.org/reference/1b735f0b98a231c622bd60352c3526be09dcb6c1" ,
  "http://www.wikidata.org/entity/statement/Q15021033-e0df04d2-489b-2d66-a804-02787694fe61" ,
  "http://fr.dbpedia.org/resource/Breakpad" ,
  "http://www.wikidata.org/entity/Q399" ,
  "http://www.wikidata.org/entity/Q571" ,
  "http://www.wikidata.org/entity/Q9418" ,
  "http://www.wikidata.org/entity/statement/Q874405-01895e4b-4724-3587-0c1a-c17c9fb90402" ,
  "http://www.wikidata.org/entity/statement/Q874405-99284362-43cf-82bc-f203-0e6c8cde82c3" ,
  "http://www.wikidata.org/entity/statement/Q874405-93303d7d-4206-e92c-96b4-473766dc1137" ,
  "http://www.wikidata.org/entity/statement/Q15021033-abdcdc96-45a6-5d11-e838-c15f3c69e5aa" ,
  "http://www.wikidata.org/entity/statement/Q874405-b77d7288-450b-ca00-a07f-41714f916f30" ,
  "http://www.wikidata.org/entity/Q8425" ,
  "http://www.wikidata.org/entity/Q34749" ,
  "http://www.wikidata.org/entity/Q302556" ,
  "http://www.wikidata.org/entity/Q8287968" ,
  "http://www.wikidata.org/entity/Q1983760" ,
  "http://www.wikidata.org/entity/statement/Q7397-48315932-5200-4FB4-BCDC-2AE9491DF3EF" ,
  "http://www.wikidata.org/entity/statement/Q7397-d91c1857-47b1-db28-3d0e-4c4f74be32a2" ,
  "http://www.wikidata.org/entity/statement/Q7397-EC27F2B5-4F77-4535-9153-C5C4CE56B35E" ,
  "http://www.wikidata.org/entity/statement/Q7397-8EB420F4-D6AD-43C1-9C22-8CE8B9FB1032" ,
  "http://www.wikidata.org/entity/statement/Q7397-0d0fb5e9-4a86-bfc7-7d42-3048dea212e0" ,
  "http://id.dbpedia.org/resource/RAID" ,
  "http://id.dbpedia.org/resource/YAML"
  )

  val httpConf = http.baseURL("http://localhost:9000")
  
  val execs = uris.map { uri =>
    exec( http( s"displayuri $uri").get( s"/display?displayuri=$uri") )    
  }
    
    val scn2 = scenario("Stress 1")
        . exec( execs )
             .pause(5)

    setUp(scn2.inject(atOnceUsers(1)))
      .protocols(httpConf)

}
