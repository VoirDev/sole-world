package dev.voir.sole.world.api.dataset.index

/**
 * Direction a sorted collection runs in.
 *
 * A caller who does not ask for one gets the direction the chosen field is useful in — most popular
 * first, names from A to Z — so the common request stays the short one.
 */
enum class SortOrder {
    ASC,
    DESC,
}
