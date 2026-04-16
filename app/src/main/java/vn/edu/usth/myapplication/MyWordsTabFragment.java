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

public class MyWordsTabFragment extends Fragment {

    private final List<LearnedWordEntity> words = new ArrayList<>();
    private MyWordsAdapter adapter;
    private AppRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_tab_my_words, container, false);

        repository = new AppRepository(requireContext());

        RecyclerView recycler = v.findViewById(R.id.recycler_my_words);
        TextView txtCount = v.findViewById(R.id.txt_word_count);
        TextView txtEmpty = v.findViewById(R.id.txt_empty_words);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MyWordsAdapter(words);
        recycler.setAdapter(adapter);

        String email = new UserDatabase(requireContext()).getLoggedInEmail();
        if (email != null) {
            repository.getFavoriteWordsLive(email)
                    .observe(getViewLifecycleOwner(), list -> {
                        words.clear();
                        words.addAll(list);
                        adapter.notifyDataSetChanged();
                        txtCount.setText("My Words: " + list.size() + "/50");
                        txtEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    });
        }

        return v;
    }
}