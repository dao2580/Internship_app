package vn.edu.usth.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.myapplication.data.AppRepository;
import vn.edu.usth.myapplication.data.entity.LearnedWordEntity;

public class HistoryTabFragment extends Fragment {

    private final List<String[]> items = new ArrayList<>();
    private HistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_tab_history, container, false);

        RecyclerView recycler = v.findViewById(R.id.recycler_history);
        TextView txtEmpty = v.findViewById(R.id.txt_empty_history);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistoryAdapter(items);
        recycler.setAdapter(adapter);

        String email = new UserDatabase(requireContext()).getLoggedInEmail();
        if (email != null) {
            new AppRepository(requireContext())
                    .getAllWordsLive(email)
                    .observe(getViewLifecycleOwner(), list -> {
                        items.clear();
                        for (LearnedWordEntity e : list) {
                            String vi = (e.translated != null && !e.translated.isEmpty())
                                    ? e.translated : e.labelVi;
                            items.add(new String[]{e.labelEn, vi});
                        }
                        adapter.notifyDataSetChanged();
                        if (txtEmpty != null) {
                            txtEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    });
        }
        return v;
    }
}