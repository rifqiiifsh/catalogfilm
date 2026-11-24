package com.katalogfilm.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.katalogfilm.app.R;
import com.katalogfilm.app.data.Repository;
import com.katalogfilm.app.data.local.Entities;
import com.katalogfilm.app.databinding.ActivityNotificationsBinding;
import com.katalogfilm.app.util.SoundManager;
import com.katalogfilm.app.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends BaseActivity {

    private ActivityNotificationsBinding b;
    private List<Entities.Notif> all = new ArrayList<>();
    private final Adapter adapter = new Adapter();

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        Utils.insets(b.root, true, true);
        Repository repo = Repository.get(this);

        b.list.setLayoutManager(new LinearLayoutManager(this));
        b.list.setAdapter(adapter);
        b.btnBack.setOnClickListener(v -> finish());
        b.btnReadAll.setOnClickListener(v -> { SoundManager.tap(v); repo.markAllRead(); });
        b.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { SoundManager.click(NotificationsActivity.this); render(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });

        repo.notifs().observe(this, list -> {
            all = list == null ? new ArrayList<>() : list;
            render();
        });
    }

    private void render() {
        int pos = b.tabs.getSelectedTabPosition();
        String cat = pos == 1 ? "Film" : pos == 2 ? "Sistem" : null;
        List<Entities.Notif> out = new ArrayList<>();
        for (Entities.Notif n : all) if (cat == null || cat.equals(n.category)) out.add(n);
        adapter.submit(out);
        b.empty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        private final List<Entities.Notif> items = new ArrayList<>();

        void submit(List<Entities.Notif> l) { items.clear(); items.addAll(l); notifyDataSetChanged(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView icon, title, body, time; final View dot;
            VH(View v) {
                super(v);
                icon = v.findViewById(R.id.icon);
                title = v.findViewById(R.id.title);
                body = v.findViewById(R.id.body);
                time = v.findViewById(R.id.time);
                dot = v.findViewById(R.id.dot);
            }
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_notification, p, false));
        }

        @Override public void onBindViewHolder(@NonNull VH h, int i) {
            Entities.Notif n = items.get(i);
            h.icon.setText("Film".equals(n.category) ? "🎬" : "🔔");
            h.title.setText(n.title);
            h.body.setText(n.body);
            h.time.setText(Utils.ago(n.createdAt));
            h.dot.setVisibility(n.isRead ? View.INVISIBLE : View.VISIBLE);
        }

        @Override public int getItemCount() { return items.size(); }
    }
}
