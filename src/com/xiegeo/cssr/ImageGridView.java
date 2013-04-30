package com.xiegeo.cssr;

import java.util.Arrays;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;

import com.xiegeo.cssr.analyze.CssImage;

public class ImageGridView extends GridView {

	public ImageGridView(Context context) {
		super(context);
	}
	
	public void setImages(CssImage[] images) {
		ImageAdapter a = (ImageAdapter) getAdapter();
		if (a== null) {
			setAdapter(new ImageAdapter(images));
		}else {
			a.setImages(images);
		}
	}

}
class ImageAdapter extends BaseAdapter {

	private CssImage[] mImages;
	ImageAdapter(CssImage[] images){
		super();
		mImages=images;
	}
	
	public void setImages(CssImage[] mImages) {
		this.mImages = mImages;
		notifyDataSetChanged();
	}
	
	public int getCount() {
		return mImages.length;
	}

	public CssImage getItem(int index) {
		return mImages[index];
	}

	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public View getView(int index, View oldView, ViewGroup group) {
		if (oldView != null && ((CssImageView)oldView).getImage() == getItem(index)) {
			return oldView;
		}
		return new CssImageView(getItem(index), MainActivity.getIconSide(), group.getContext());
	}
	
}
class CssImageView extends View{

	private CssImage mImage;
	int preSide;
	int side;
	final int iconNumber;
	public CssImageView(CssImage image, int side, Context context) {
		super(context);
		iconNumber = iconCount++;
		mImage=image;
		this.preSide = side;
		this.side = side;
		setBackgroundColor(Color.TRANSPARENT);
	}
	@Override
	public void draw(Canvas canvas) {
		if (mImage != null) {
			canvas.drawBitmap(mImage.getImage(side), 0, side, 0, 0, side, side, true, null);
		}
	}
	public void setImage(CssImage image) {
		mImage = image;
		invalidate();
	}
	public CssImage getImage() {
		return mImage;
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(side, side);
	}
	
	@Override
	public Bitmap getDrawingCache() {
		return getDrawingCache(false);
	}
	@Override
	public Bitmap getDrawingCache(boolean autoScale) {
		// TODO Auto-generated method stub
		Bitmap icon = super.getDrawingCache(autoScale);
		if (icon != null) {
			addDrawingCache(icon,iconNumber);
		}
		return icon;
	}
	private static int iconCount = 0;
	private static Bitmap[] iconCache = new Bitmap[64];
	private static void addDrawingCache(Bitmap icon, int iconNumber) {
		iconCache[iconNumber%iconCache.length]=icon;
	}
	static void clearCache() {
		Arrays.fill(iconCache, null);
	}
}