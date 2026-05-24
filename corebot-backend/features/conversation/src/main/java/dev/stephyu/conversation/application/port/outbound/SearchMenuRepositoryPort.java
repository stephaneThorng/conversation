package dev.stephyu.conversation.application.port.outbound;

import dev.stephyu.conversation.domain.menu.MenuItemSearchQuery;
import dev.stephyu.conversation.domain.menu.MenuItemSearchResult;
import dev.stephyu.conversation.domain.menu.MenuSearchQuery;
import dev.stephyu.conversation.domain.menu.MenuSearchResult;
import java.util.List;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface SearchMenuRepositoryPort {

    List<MenuSearchResult> searchMenus(MenuSearchQuery query);

    List<MenuItemSearchResult> searchMenuItems(MenuItemSearchQuery query);
}
