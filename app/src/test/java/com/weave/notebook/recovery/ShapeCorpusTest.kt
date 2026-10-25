package com.weave.notebook.recovery

import com.weave.notebook.model.*
import com.weave.notebook.shape.ShapeRecognizer
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*
import kotlin.random.Random

class ShapeCorpusTest {
    @Test fun seededNoisyPositiveCorpus_andScribbleNegatives() {
        val recognizer=ShapeRecognizer()
        val results=mutableMapOf<String,Int>()
        for(kind in listOf("line","circle","ellipse","triangle","rectangle","square")) {
            var matched=0
            repeat(20) { seed->
                val random=Random(seed+120)
                val base:List<Pair<Float,Float>> = when(kind) {
                    "line"->(0..60).map {it.toFloat() to 0f}
                    "circle","ellipse"->(0..72).map {i->val angle=i*2*PI/72;(cos(angle)*20).toFloat() to (sin(angle)*if(kind=="circle")20 else 9).toFloat()}
                    else->{val vertices=when(kind) {"triangle"->listOf(0f to 0f,30f to 0f,15f to 25f,0f to 0f);"rectangle"->listOf(0f to 0f,40f to 0f,40f to 20f,0f to 20f,0f to 0f);else->listOf(0f to 0f,30f to 0f,30f to 30f,0f to 30f,0f to 0f)}
                        vertices.zipWithNext().flatMap { (a,b)->(0 until 16).map {i->val t=i/16f;(a.first+(b.first-a.first)*t) to (a.second+(b.second-a.second)*t)}}+vertices.last()
                    }
                }
                val angle=if(kind in listOf("triangle","rectangle","square"))seed*0.07f else 0f
                val samples=base.mapIndexed {i,p->
                    val x=p.first*cos(angle)-p.second*sin(angle)+60
                    val y=p.first*sin(angle)+p.second*cos(angle)+60
                    StrokeSample(x+random.nextFloat()*0.2f-0.1f,y+random.nextFloat()*0.2f-0.1f,i.toLong())
                }
                val s=recognizer.classify(samples)?.shape
                val ok=when(kind) {"line"->s is FittedShape.Line;"circle"->s is FittedShape.Circle;"ellipse"->s is FittedShape.Ellipse;else->s is FittedShape.Polygon && s.kind.name.lowercase()==kind}
                if(ok)matched++
            }
            results[kind]=matched
        }
        assertTrue("Positive matches $results",results.values.all {it>=18})
        var falseSnaps=0
        repeat(100) {seed->
            val points=(0..80).map {i->val t=i/80f
                StrokeSample(30+t*35,70+sin(t*PI.toFloat()*(5+seed%7))*5+sin(t*41)*2,i.toLong())}
            if(recognizer.classify(points)!=null)falseSnaps++
        }
        assertTrue("False snaps: $falseSnaps/100",falseSnaps<=2)
    }
}
