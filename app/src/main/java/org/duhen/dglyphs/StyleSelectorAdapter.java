package org.duhen.dglyphs;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StyleSelectorAdapter extends RecyclerView.Adapter<StyleSelectorAdapter.ViewHolder> {

    private final Context context;
    private final List<StyleItem> items;
    private final String vibKey;
    private final SparseArray<ViewHolder> activeHolders = new SparseArray<>();

    public StyleSelectorAdapter(Context context, List<StyleItem> items, String vibKey) {
        this.context = context;
        this.items = items;
        this.vibKey = vibKey;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_style_card, parent, false);
        return new ViewHolder(view, vibKey);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        activeHolders.put(position, holder);
        holder.bind(items.get(position));
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        int pos = holder.getBindingAdapterPosition();
        if (pos != RecyclerView.NO_POSITION) activeHolders.remove(pos);
        holder.stopAnimation();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void playAnimationAt(int position) {
        for (int i = 0; i < activeHolders.size(); i++) {
            int key = activeHolders.keyAt(i);
            ViewHolder holder = activeHolders.valueAt(i);
            if (holder == null) continue;
            if (key == position) holder.playAnimation();
            else holder.stopAnimation();
        }
    }

    public void stopAll() {
        for (int i = 0; i < activeHolders.size(); i++) {
            ViewHolder holder = activeHolders.valueAt(i);
            if (holder != null) holder.stopAnimation();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final GlyphPreviewView glyphPreview;
        private final String vibKey;
        private PreviewAnimator animator;
        private StyleItem currentItem;

        ViewHolder(View itemView, String vibKey) {
            super(itemView);
            this.vibKey = vibKey;
            this.glyphPreview = itemView.findViewById(R.id.glyphPreview);
            glyphPreview.setClickable(false);
            glyphPreview.setFocusable(false);
        }

        void bind(StyleItem item) {
            this.currentItem = item;
            if (animator == null) {
                animator = new PreviewAnimator(itemView.getContext(), glyphPreview, vibKey);
            }
        }

        void playAnimation() {
            if (currentItem != null && animator != null) {
                animator.play(currentItem.folder(), currentItem.fileName());
            }
        }

        void stopAnimation() {
            if (animator != null) animator.stop();
        }
    }
}