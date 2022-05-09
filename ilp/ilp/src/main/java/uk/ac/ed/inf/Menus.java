package uk.ac.ed.inf;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.util.ArrayList;

import com.google.gson.reflect.TypeToken;


public class Menus {
    HttpHandler httpHandler;
    String string_menus;
    ArrayList<Menu> allMenus;






    public static class Menu {
        String name;
        String location;
        Item[] menu;
        public static class Item{
            String item;
            int pence;
        }
    }
    public Menus(String localhost, String s)  {
        this.httpHandler = new HttpHandler(localhost,s);
        this.string_menus = this.httpHandler.getRequest("/menus/menus.json");
        this.allMenus = getAllMenus();
    }

    private ArrayList<Menu> getAllMenus(){
        Type listType = new TypeToken<ArrayList<Menu>>(){}.getType();
        ArrayList<Menu> menuArrayList = new Gson().fromJson(string_menus,listType);
        return menuArrayList;
    }

    public int getDeliveryCost(String... items) {
        int totalCost = 0;
        if (items.length == 0){return totalCost;}
        for (String currentItem : items){
            for (Menu currentMenu : this.allMenus){
                for(Menu.Item item : currentMenu.menu){
                    if (currentItem.equals(item.item)) {
                        totalCost += item.pence;
                    continue;}
                }
            }
        }
        return totalCost + 50;
    }
    public String getRestaurant(String... items) {
        String currentRest = new String();
        if (items.equals("")){return currentRest;}
        for (Menu currentMenu: this.allMenus){
            for (String currentItem: items){
                for (Menu.Item item: currentMenu.menu){
                    if (currentItem.equals(item.item)){
                        currentRest = currentMenu.location;
                        currentRest = currentMenu.name;
                        continue;
                    }
                }
            }
        }return currentRest;


    }
}
