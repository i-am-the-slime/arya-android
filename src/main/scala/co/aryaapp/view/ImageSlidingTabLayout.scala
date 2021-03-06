package co.aryaapp.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup.LayoutParams.{MATCH_PARENT => Match, WRAP_CONTENT => Wrap}
import co.aryaapp.java.SlidingTabLayout

class ImageSlidingTabLayout(ctx:Context, attrs:AttributeSet, style:Int) extends SlidingTabLayout(ctx, attrs, style) {
  def this(c:Context, a:AttributeSet) = this(c, a, 0)

  var viewFiller:(Int, View) => View = _

  def setViewFiller(filler:(Int, View) => View) = {
    viewFiller = filler
  }

  override protected def populateTabStrip() = {
    super.populateTabStrip()

    for (index <- 0 until mTabStrip.getChildCount) {
      val child = mTabStrip.getChildAt(index)
      viewFiller(index, child)
    }
  }
}
