package vn.edu.usth.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.myapplication.data.AppRepository;
import vn.edu.usth.myapplication.data.entity.LearnedWordEntity;

public class HistoryTabFragment extends Fragment implements HistoryAdapter.OnFavoriteClickListener {

    private final List<LearnedWordEntity> items = new ArrayList<>();
    private HistoryAdapter adapter;
    private AppRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_tab_history, container, false);

        repository = new AppRepository(requireContext());

        RecyclerView recycler = v.findViewById(R.id.recycler_history);
        TextView txtEmpty = v.findViewById(R.id.txt_empty_history);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistoryAdapter(items, this);
        recycler.setAdapter(adapter);

        String email = new UserDatabase(requireContext()).getLoggedInEmail();
        if (email != null) {
            repository.getHistoryWordsLive(email)
                    .observe(getViewLifecycleOwner(), list -> {
                        items.clear();
                        items.addAll(list);
                        adapter.notifyDataSetChanged();
                        txtEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    });
        }

        return v;
    }

    @Override
    public void onFavoriteClick(LearnedWordEntity item) {
        boolean newValue = !item.isFavorite;
        repository.setFavorite(item.id, newValue);

        Toast.makeText(
                requireContext(),
                newValue ? "Đã thêm vào My Words" : "Đã bỏ khỏi My Words",
                Toast.LENGTH_SHORT
        ).show();
    }
}