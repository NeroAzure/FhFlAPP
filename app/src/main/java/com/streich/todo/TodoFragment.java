package com.streich.todo;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.internal.NavigationMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;


import com.example.antonimuller.fhflapp.R;

import java.util.ArrayList;

import io.github.yavski.fabspeeddial.FabSpeedDial;

import static android.R.attr.x;

/**
 * Created by Sebastian Streich  on 25.10.2016.
 */

public class TodoFragment extends Fragment implements
        View.OnClickListener, AdapterView.OnItemClickListener,
        FabSpeedDial.MenuListener, CreateCategoryDialog.CategoryListner,
        SelectCategoryDialog.SelectionListener,
        CreateTodoDialog.CreateTodoListener, AdapterView.OnItemLongClickListener {
    final static String key ="Todo-Fragment";


    TodoListHolder  model;
    TodoListAdapter listAdapter;
    ArrayList<TodoModel> viewModel;
    //ViewElements
    View            fragmentView;
    ListView        todoList;
    Button selectorButton;
    CreateCategoryDialog   createCategoryDialog;
    SelectCategoryDialog   selectCategoryDialog;
    CreateTodoDialog       createTodoDialog;

    String selectedCategory;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.todo_fragment, container, false);

        //Fetch the UI Components needed.
        todoList                    = (ListView)     fragmentView.findViewById(R.id.listV);
        selectorButton              = (Button)       fragmentView.findViewById(R.id.todo_selection_button);
        FabSpeedDial fabSpeedDial   = (FabSpeedDial) fragmentView.findViewById(R.id.todo_appButton);

        //Initalise the Dialogues
        createCategoryDialog = new CreateCategoryDialog();
        selectCategoryDialog = new SelectCategoryDialog();
        createTodoDialog     = new CreateTodoDialog();

        selectCategoryDialog.setAllVisibility(true);
        createTodoDialog.setCreateTodoListener(this);
        //Fetch the Model
        model = TodoListHolder.getMe();
        model.fetch(getActivity());
        selectedCategory="Alle";

        //Connect the Model to the UI
        listAdapter = new TodoListAdapter(getActivity(), R.layout.todo_row, model);
        todoList.setAdapter(listAdapter);

        //Set EventListners
        todoList.setOnItemClickListener(this);
        todoList.setOnItemLongClickListener(this);
        selectorButton.setOnClickListener(this);
        createCategoryDialog.setCategoryListner(this);
        fabSpeedDial.setMenuListener(this);
        selectCategoryDialog.setSelectionListener(this);
        return fragmentView;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Log.v(key,"Clicked on an Item");
        TodoModel clickedItem = listAdapter.getItem(position);
        clickedItem.done= !clickedItem.done;
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final TodoModel clickedItem = listAdapter.getItem(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Todo Löschen?");
        builder.setMessage("Möchten sie '"+ clickedItem.title +"' wirklich Löschen?");
        builder.setNegativeButton("Nein", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.v(key,"Not Deleting selected Item");
            }
        });
        builder.setPositiveButton("Ja Löschen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                Log.v(key,"Deleting selected Item");
                if( !selectedCategory.equals("Alle")  && clickedItem.category.equals(selectedCategory)) {
                    viewModel.remove(clickedItem);
                }
                model.remove(clickedItem);

                listAdapter.notifyDataSetChanged();
            }
        });
        builder.show();
        return false;
    }


    @Override
    public boolean onMenuItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()){
            case R.id.todo_new_category:
                createCategoryDialog.show(getFragmentManager(),"");
                break;
            case R.id.todo_new_todo:
                Log.v(key,"New Todo");
                createTodoDialog.show(getFragmentManager(),"");

                break;
        }
        return false;
    }

    public void onMenuClosed(){}


    @Override
    public void onPause(){
        Log.v(key,"Pausing the Fragment");
        super.onPause();
        //Pausing the Activity.
        Context con = getActivity();
        model.commmit(con);

    }
    @Override
    public void onNewCategory(String Category) {
        Log.v("Todo-Fragment","New Category Requested:" + Category);
        model.getCategorys().add(Category);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.todo_selection_button){selectCategoryDialog.show(getFragmentManager(),"");}
    }


    @Override
    public boolean onPrepareMenu(NavigationMenu navigationMenu) {
        return true;
    }

    @Override
    public void OnSelectionChanged(String Category, int Index) {
        selectedCategory = Category;


        if(Index <0){
            selectorButton.setText("Alle");
            viewModel = model;
        }else{
            selectorButton.setText(Category);
            viewModel = new ArrayList<>();
            //Create a sublist of the Model
            //Add all Todos matching the Category
            for(int i=0; i< model.size(); i++){
                TodoModel target = model.get(i);
                if(target.category.equals(Category)){
                    viewModel.add(target);
                }

            }

        }

        listAdapter = new TodoListAdapter(getActivity(), R.layout.todo_row, viewModel);
        todoList.setAdapter(listAdapter);
    }

    @Override
    public void OnTodoCreated(TodoModel t) {
        model.add(t);
        if(selectedCategory != "Alle" && t.category == selectedCategory){
            viewModel.add(t);
        }
        listAdapter.notifyDataSetChanged();
    }


}
