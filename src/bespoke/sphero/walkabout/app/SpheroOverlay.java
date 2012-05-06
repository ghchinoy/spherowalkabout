package bespoke.sphero.walkabout.app;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class SpheroOverlay extends ItemizedOverlay<OverlayItem> {

	private ArrayList<OverlayItem> itemList = new ArrayList<OverlayItem>();

	public SpheroOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
		populate();
	}

	public void addItem(GeoPoint p, String title, String snippet) {
		OverlayItem newItem = new OverlayItem(p, title, snippet);
		itemList.add(newItem);
		populate();
	}

	@Override
	protected OverlayItem createItem(int i) {
		return null;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);
	}

}
