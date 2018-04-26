package ru.agroexpert2007.aegis;

import android.util.Log;

import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayWithIW;

import java.util.Iterator;
import java.util.List;

public class OverlaysHandler {
    public static void removeOverlay(List<Overlay> overlayList, int overlayIndex) {
        overlayList.remove(overlayIndex);
    }
    public static void removeAllMarkers(List<Overlay> overlayList) {
        for(Overlay o: overlayList) {
            if(o.toString().contains("org.osmdroid.views.overlay.Marker")) {
                overlayList.remove(o);
            }
        }
    }
}