package com.weave.notebook

import android.graphics.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import com.weave.notebook.ui.NativeUi
import com.google.android.material.color.MaterialColors
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ThemeAssetTest {
    private val inst=InstrumentationRegistry.getInstrumentation()
    @Test fun controlsResolveTheActivityPaletteInsteadOfHardcodedBrandColors() {
        val intent=android.content.Intent(inst.targetContext,com.weave.notebook.ui.LibraryActivity::class.java)
        androidx.test.core.app.ActivityScenario.launch<com.weave.notebook.ui.LibraryActivity>(intent).use {scenario->
            scenario.onActivity {activity->
                try {
                    activity.theme.applyStyle(R.style.ThemeOverlay_Weave_PaletteVerification,true)
                    NativeUi.applyTheme(activity)
                    assertEquals(Color.rgb(115,80,164),NativeUi.TEAL)
                    assertEquals(Color.rgb(252,248,255),NativeUi.BG)
                    assertEquals(Color.rgb(32,26,37),NativeUi.TEXT)
                    val button=NativeUi.button(activity,"Selected",true) {}
                    assertEquals(NativeUi.ON_TEAL,button.currentTextColor)
                    assertEquals(NativeUi.TEAL,(button.background as android.graphics.drawable.GradientDrawable).color!!.defaultColor)
                } finally {
                    activity.setTheme(R.style.Theme_Weave)
                    com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(activity)
                    NativeUi.applyTheme(activity)
                }
            }
        }
    }
    @Test fun supportedAndroidUsesSystemPaletteAndExposesThemedLauncherLayer() {
        org.junit.Assume.assumeTrue(android.os.Build.VERSION.SDK_INT>=33)
        assertTrue(com.google.android.material.color.DynamicColors.isDynamicColorAvailable())
        val intent=android.content.Intent(inst.targetContext,com.weave.notebook.ui.LibraryActivity::class.java)
        androidx.test.core.app.ActivityScenario.launch<com.weave.notebook.ui.LibraryActivity>(intent).use {scenario->scenario.onActivity {activity->
            val night=activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK==android.content.res.Configuration.UI_MODE_NIGHT_YES
            val expected=activity.getColor(if(android.os.Build.VERSION.SDK_INT>=34) {if(night)android.R.color.system_primary_dark else android.R.color.system_primary_light} else {if(night)android.R.color.system_accent1_200 else android.R.color.system_accent1_600})
            assertEquals("Activity uses the system wallpaper accent",expected,NativeUi.TEAL)
            val icon=activity.packageManager.getApplicationIcon(activity.packageName) as android.graphics.drawable.AdaptiveIconDrawable
            assertNotNull(icon.monochrome)
            val bitmap=Bitmap.createBitmap(432,432,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap)
            val bg=Paint().apply {color=NativeUi.SELECTED};canvas.drawCircle(216f,216f,144f,bg)
            icon.monochrome!!.mutate().apply {setTint(NativeUi.TEAL);setBounds(0,0,432,432);draw(canvas)}
            File(activity.getExternalFilesDir(null),"weave-material-you-icon.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        }}
    }
    @Test fun logoIsTransparentAndAdaptiveLayersStayInsideCircularSafeZone() {inst.runOnMainSync {
        val context=ContextThemeWrapper(inst.targetContext,R.style.Theme_Weave)
        fun render(id:Int,size:Int):Bitmap {
            val width=if(id==R.drawable.weave_owl)size*376/427 else size
            val image=Bitmap.createBitmap(width,size,Bitmap.Config.ARGB_8888)
            context.getDrawable(id)!!.apply {setBounds(0,0,width,size);draw(Canvas(image))}
            return image
        }
        val logo=render(R.drawable.weave_owl,1280)
        assertEquals(0,Color.alpha(logo.getPixel(0,0)));assertEquals(0,Color.alpha(logo.getPixel(logo.width-1,logo.height-1)))
        File(context.getExternalFilesDir(null),"weave-owl-vector-render.png").outputStream().use {logo.compress(Bitmap.CompressFormat.PNG,100,it)};logo.recycle()
        for(id in listOf(R.drawable.ic_launcher_foreground,R.drawable.ic_launcher_monochrome)) {
            val bitmap=render(id,432)
            for(y in 0 until 432)for(x in 0 until 432)if(Color.alpha(bitmap.getPixel(x,y))>0) {
                val dx=x-216f;val dy=y-216f
                assertTrue("Logo extends outside adaptive safe circle at $x,$y",dx*dx+dy*dy<=132f*132f)
            }
            val name=if(id==R.drawable.ic_launcher_monochrome)"weave-themed-owl.png" else "weave-adaptive-owl.png"
            File(context.getExternalFilesDir(null),name).outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        }
    }}
}
