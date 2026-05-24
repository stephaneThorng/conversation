insert into restaurant_menu_item_category (code, name_translations, active)
values
    ('starter', '{"en":"Starter","fr":"Entree"}', true),
    ('main_course', '{"en":"Main Course","fr":"Plat principal"}', true),
    ('dessert', '{"en":"Dessert","fr":"Dessert"}', true),
    ('drink', '{"en":"Drink","fr":"Boisson"}', true),
    ('indonesian', '{"en":"Indonesian","fr":"Indonesien"}', true),
    ('grilled', '{"en":"Grilled","fr":"Grille"}', true),
    ('burger', '{"en":"Burger","fr":"Burger"}', true),
    ('fermented', '{"en":"Fermented","fr":"Fermente"}', true)
on conflict (code) do update
set name_translations = excluded.name_translations,
    active = excluded.active,
    updated_at = now();

insert into restaurant_allergen (code, name_translations, active)
values
    ('gluten', '{"en":"Gluten","fr":"Gluten"}', true),
    ('soy', '{"en":"Soy","fr":"Soja"}', true),
    ('peanut', '{"en":"Peanut","fr":"Arachide"}', true),
    ('crustacean', '{"en":"Crustacean","fr":"Crustace"}', true),
    ('egg', '{"en":"Egg","fr":"Oeuf"}', true),
    ('dairy', '{"en":"Dairy","fr":"Lait"}', true),
    ('sesame', '{"en":"Sesame","fr":"Sesame"}', true),
    ('fish', '{"en":"Fish","fr":"Poisson"}', true),
    ('tree_nut', '{"en":"Tree Nut","fr":"Fruit a coque"}', true)
on conflict (code) do update
set name_translations = excluded.name_translations,
    active = excluded.active,
    updated_at = now();

insert into restaurant_dietary_restriction (code, name_translations, active)
values
    ('vegan', '{"en":"Vegan","fr":"Vegan"}', true),
    ('gluten_free', '{"en":"Gluten-Free","fr":"Sans gluten"}', true),
    ('vegetarian', '{"en":"Vegetarian","fr":"Vegetarien"}', true),
    ('contains_alcohol', '{"en":"Contains Alcohol","fr":"Contient de l''alcool"}', true)
on conflict (code) do update
set name_translations = excluded.name_translations,
    active = excluded.active,
    updated_at = now();

insert into restaurant_establishment (
    id,
    code,
    default_locale,
    name_translations,
    description_translations,
    active
)
values (
    '6f7d2c8e-2f55-4e75-8c9a-2f31fdd9b101',
    'warung_steven',
    'en',
    '{"en":"Warung Steven","fr":"Warung Steven"}',
    '{"en":"Casual Balinese warung serving Indonesian comfort food and fresh drinks.","fr":"Warung balinais convivial servant une cuisine indonesienne genereuse et des boissons fraiches."}',
    true
)
on conflict (id) do update
set code = excluded.code,
    default_locale = excluded.default_locale,
    name_translations = excluded.name_translations,
    description_translations = excluded.description_translations,
    active = excluded.active,
    updated_at = now();

insert into restaurant_menu (
    id,
    establishment_id,
    code,
    name_translations,
    description_translations,
    active,
    sort_order,
    price_cents,
    currency
)
values
    (
        '43d4c90d-4a2e-4d5e-9ed0-f9c3cf91d201',
        '6f7d2c8e-2f55-4e75-8c9a-2f31fdd9b101',
        'a_la_carte',
        '{"en":"A La Carte","fr":"A la carte"}',
        '{"en":"Daily menu available all day.","fr":"Carte quotidienne disponible toute la journee."}',
        true,
        1,
        null,
        'EUR'
    ),
    (
        '43d4c90d-4a2e-4d5e-9ed0-f9c3cf91d202',
        '6f7d2c8e-2f55-4e75-8c9a-2f31fdd9b101',
        'chef_specials',
        '{"en":"Chef Specials","fr":"Suggestions du chef"}',
        '{"en":"Signature dishes and seasonal catches.","fr":"Plats signatures et selections de saison."}',
        true,
        2,
        null,
        'EUR'
    )
on conflict (id) do update
set establishment_id = excluded.establishment_id,
    code = excluded.code,
    name_translations = excluded.name_translations,
    description_translations = excluded.description_translations,
    active = excluded.active,
    sort_order = excluded.sort_order,
    price_cents = excluded.price_cents,
    currency = excluded.currency,
    updated_at = now();

insert into restaurant_menu_section (
    id,
    menu_id,
    code,
    name_translations,
    description_translations,
    sort_order,
    active
)
values
    (
        '1d3c6f30-58ab-4ff4-bbb2-95e4c8652201',
        '43d4c90d-4a2e-4d5e-9ed0-f9c3cf91d201',
        'starter',
        '{"en":"Starters","fr":"Entrees"}',
        '{"en":"Small plates to begin your meal.","fr":"Petites assiettes pour commencer le repas."}',
        1,
        true
    ),
    (
        '1d3c6f30-58ab-4ff4-bbb2-95e4c8652202',
        '43d4c90d-4a2e-4d5e-9ed0-f9c3cf91d201',
        'main_course',
        '{"en":"Main Courses","fr":"Plats principaux"}',
        '{"en":"Hearty Indonesian mains and house favorites.","fr":"Plats indonesiens genereux et specialites de la maison."}',
        2,
        true
    ),
    (
        '1d3c6f30-58ab-4ff4-bbb2-95e4c8652203',
        '43d4c90d-4a2e-4d5e-9ed0-f9c3cf91d201',
        'dessert',
        '{"en":"Desserts","fr":"Desserts"}',
        '{"en":"Sweet finishes inspired by local ingredients.","fr":"Douceurs de fin de repas inspirees des produits locaux."}',
        3,
        true
    ),
    (
        '1d3c6f30-58ab-4ff4-bbb2-95e4c8652204',
        '43d4c90d-4a2e-4d5e-9ed0-f9c3cf91d201',
        'drink',
        '{"en":"Drinks","fr":"Boissons"}',
        '{"en":"Refreshing house-made and local beverages.","fr":"Boissons maison et rafraichissements locaux."}',
        4,
        true
    ),
    (
        '1d3c6f30-58ab-4ff4-bbb2-95e4c8652205',
        '43d4c90d-4a2e-4d5e-9ed0-f9c3cf91d202',
        'featured',
        '{"en":"Featured Specials","fr":"Plats a l''honneur"}',
        '{"en":"Rotating specials highlighted by the kitchen.","fr":"Suggestions tournantes mises en avant par la cuisine."}',
        1,
        true
    )
on conflict (id) do update
set menu_id = excluded.menu_id,
    code = excluded.code,
    name_translations = excluded.name_translations,
    description_translations = excluded.description_translations,
    sort_order = excluded.sort_order,
    active = excluded.active,
    updated_at = now();

insert into restaurant_menu_item (
    id,
    establishment_id,
    code,
    name_translations,
    description_translations,
    ingredient_note_translations,
    price_cents,
    currency,
    category_codes,
    allergen_codes,
    dietary_restriction_codes,
    active
)
values
    (
        '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093101',
        '6f7d2c8e-2f55-4e75-8c9a-2f31fdd9b101',
        'chicken_spring_rolls',
        '{"en":"Chicken Spring Rolls (x4)","fr":"Rouleaux de printemps au poulet (x4)"}',
        '{"en":"Traditional fried chicken and vegetable spring rolls, served with sweet and sour sauce.","fr":"Rouleaux de printemps frits au poulet et aux legumes, servis avec une sauce aigre-douce."}',
        '{"en":"Chicken, cabbage, carrot, garlic, wheat wrapper, soy seasoning.","fr":"Poulet, chou, carotte, ail, galette de ble, assaisonnement soja."}',
        400,
        'EUR',
        '{"starter","indonesian"}',
        '{"gluten","soy"}',
        '{}',
        true
    ),
    (
        '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093102',
        '6f7d2c8e-2f55-4e75-8c9a-2f31fdd9b101',
        'green_papaya_salad',
        '{"en":"Green Papaya Salad","fr":"Salade de papaye verte"}',
        '{"en":"Crisp green papaya, carrots, cherry tomatoes, lime dressing.","fr":"Papaye verte croquante, carottes, tomates cerises et vinaigrette au citron vert."}',
        '{"en":"Green papaya, carrot, cherry tomato, palm sugar, lime juice, herbs.","fr":"Papaye verte, carotte, tomate cerise, sucre de palme, jus de citron vert, herbes."}',
        300,
        'EUR',
        '{"starter","indonesian"}',
        '{}',
        '{"vegan","gluten_free"}',
        true
    ),
    (
        '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093103',
        '6f7d2c8e-2f55-4e75-8c9a-2f31fdd9b101',
        'chicken_satay_skewers',
        '{"en":"Chicken Satay Skewers (x5)","fr":"Brochettes de satay au poulet (x5)"}',
        '{"en":"Wood-fired grilled chicken skewers, thick peanut sauce.","fr":"Brochettes de poulet grillees au feu de bois, sauce cacahuete onctueuse."}',
        '{"en":"Chicken thigh, lemongrass marinade, peanut sauce, soy glaze.","fr":"Haut de cuisse de poulet, marinade citronnelle, sauce cacahuete, glacage soja."}',
        500,
        'EUR',
        '{"starter","grilled","indonesian"}',
        '{"peanut","soy"}',
        '{}',
        true
    ),
    (
        '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093104',
        '6f7d2c8e-2f55-4e75-8c9a-2f31fdd9b101',
        'nasi_goreng_special',
        '{"en":"Nasi Goreng Special","fr":"Nasi goreng special"}',
        '{"en":"Indonesian fried rice, fried egg, chicken, satay skewer, prawn crackers.","fr":"Riz frit indonesien, oeuf au plat, poulet, brochette satay et crackers de crevettes."}',
        '{"en":"Jasmine rice, chicken, egg, sweet soy, sambal, satay skewer, prawn crackers.","fr":"Riz jasmin, poulet, oeuf, sauce soja sucree, sambal, brochette satay, crackers de crevettes."}',
        600,
        'EUR',
        '{"main_course","indonesian"}',
        '{"crustacean","egg","gluten","peanut"}',
        '{}',
        true
    ),
    (
        '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093105',
        '6f7d2c8e-2f55-4e75-8c9a-2f31fdd9b101',
        'yellow_tofu_curry',
        '{"en":"Yellow Tofu Curry","fr":"Curry jaune au tofu"}',
        '{"en":"Mild coconut milk, local vegetables, fresh tofu, served with fragrant rice.","fr":"Curry doux au lait de coco, legumes locaux et tofu frais, servi avec du riz parfume."}',
        '{"en":"Tofu, coconut milk, turmeric, green beans, carrot, kaffir lime, jasmine rice.","fr":"Tofu, lait de coco, curcuma, haricots verts, carotte, citron kaffir, riz jasmin."}',
        600,
        'EUR',
        '{"main_course","indonesian"}',
        '{"soy"}',
        '{"vegan","gluten_free"}',
        true
    ),
    (
        '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093106',
        '6f7d2c8e-2f55-4e75-8c9a-2f31fdd9b101',
        'bali_wagyu_burger',
        '{"en":"Bali Wagyu Burger","fr":"Burger wagyu de Bali"}',
        '{"en":"Wagyu beef patty, aged cheddar, brioche bun, spicy mayo, french fries.","fr":"Steak wagyu, cheddar affine, bun brioche, mayonnaise epicee et frites."}',
        '{"en":"Wagyu beef, cheddar, brioche bun, lettuce, tomato, spicy mayo, fries, sesame bun.","fr":"Boeuf wagyu, cheddar, pain brioche, salade, tomate, mayonnaise epicee, frites, pain au sesame."}',
        1200,
        'EUR',
        '{"main_course","burger"}',
        '{"gluten","dairy","egg","sesame"}',
        '{}',
        true
    ),
    (
        '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093107',
        '6f7d2c8e-2f55-4e75-8c9a-2f31fdd9b101',
        'grilled_mahi_mahi',
        '{"en":"Grilled Mahi-Mahi","fr":"Mahi-mahi grille"}',
        '{"en":"Fresh morning-caught Mahi-Mahi fillet, sambal matah sauce, white rice.","fr":"Filet de mahi-mahi du matin, sauce sambal matah et riz blanc."}',
        '{"en":"Mahi-mahi fillet, shallot sambal matah, lime, rice, local herbs.","fr":"Filet de mahi-mahi, sambal matah a l''echalote, citron vert, riz, herbes locales."}',
        900,
        'EUR',
        '{"main_course","grilled","indonesian"}',
        '{"fish"}',
        '{"gluten_free"}',
        false
    ),
    (
        '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093108',
        '6f7d2c8e-2f55-4e75-8c9a-2f31fdd9b101',
        'mango_sticky_rice',
        '{"en":"Mango Sticky Rice","fr":"Riz gluant a la mangue"}',
        '{"en":"Warm sticky rice, fresh local mango, sweet coconut cream.","fr":"Riz gluant tiede, mangue locale fraiche et creme de coco sucree."}',
        '{"en":"Sticky rice, mango, coconut cream, palm sugar, pandan leaf.","fr":"Riz gluant, mangue, creme de coco, sucre de palme, feuille de pandan."}',
        400,
        'EUR',
        '{"dessert","indonesian"}',
        '{}',
        '{"vegan","gluten_free"}',
        true
    ),
    (
        '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093109',
        '6f7d2c8e-2f55-4e75-8c9a-2f31fdd9b101',
        'chocolate_lava_cake',
        '{"en":"Chocolate Lava Cake","fr":"Moelleux au chocolat coeur coulant"}',
        '{"en":"Dark chocolate cake with a molten center, vanilla ice cream scoop.","fr":"Gateau au chocolat noir au coeur coulant, servi avec une boule de glace vanille."}',
        '{"en":"Dark chocolate, butter, egg, flour, vanilla ice cream, almond crumble.","fr":"Chocolat noir, beurre, oeuf, farine, glace vanille, crumble d''amande."}',
        500,
        'EUR',
        '{"dessert"}',
        '{"gluten","egg","dairy","tree_nut"}',
        '{}',
        true
    ),
    (
        '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093110',
        '6f7d2c8e-2f55-4e75-8c9a-2f31fdd9b101',
        'homemade_kombucha',
        '{"en":"Homemade Kombucha","fr":"Kombucha maison"}',
        '{"en":"Sparkling fermented tea, ginger and freshly squeezed lemon flavor.","fr":"The fermente petillant aux saveurs de gingembre et citron fraichement presse."}',
        '{"en":"Black tea, kombucha culture, ginger, lemon juice, cane sugar.","fr":"The noir, culture de kombucha, gingembre, jus de citron, sucre de canne."}',
        300,
        'EUR',
        '{"drink","fermented"}',
        '{}',
        '{"vegan","gluten_free"}',
        true
    ),
    (
        '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093111',
        '6f7d2c8e-2f55-4e75-8c9a-2f31fdd9b101',
        'bintang_radler',
        '{"en":"Bintang Radler","fr":"Bintang Radler"}',
        '{"en":"Light lager beer with lemon juice.","fr":"Biere blonde legere melangee a du jus de citron."}',
        '{"en":"Bintang lager, lemon juice, carbonation.","fr":"Biere Bintang, jus de citron, gaz carbonique."}',
        400,
        'EUR',
        '{"drink"}',
        '{"gluten"}',
        '{"contains_alcohol"}',
        true
    )
on conflict (id) do update
set establishment_id = excluded.establishment_id,
    code = excluded.code,
    name_translations = excluded.name_translations,
    description_translations = excluded.description_translations,
    ingredient_note_translations = excluded.ingredient_note_translations,
    price_cents = excluded.price_cents,
    currency = excluded.currency,
    category_codes = excluded.category_codes,
    allergen_codes = excluded.allergen_codes,
    dietary_restriction_codes = excluded.dietary_restriction_codes,
    active = excluded.active,
    updated_at = now();

insert into restaurant_menu_section_item_map (
    menu_section_id,
    menu_item_id,
    sort_order,
    price_cents_override
)
values
    ('1d3c6f30-58ab-4ff4-bbb2-95e4c8652201', '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093101', 1, null),
    ('1d3c6f30-58ab-4ff4-bbb2-95e4c8652201', '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093102', 2, null),
    ('1d3c6f30-58ab-4ff4-bbb2-95e4c8652201', '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093103', 3, null),
    ('1d3c6f30-58ab-4ff4-bbb2-95e4c8652202', '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093104', 1, null),
    ('1d3c6f30-58ab-4ff4-bbb2-95e4c8652202', '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093105', 2, null),
    ('1d3c6f30-58ab-4ff4-bbb2-95e4c8652202', '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093106', 3, null),
    ('1d3c6f30-58ab-4ff4-bbb2-95e4c8652202', '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093107', 4, null),
    ('1d3c6f30-58ab-4ff4-bbb2-95e4c8652203', '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093108', 1, null),
    ('1d3c6f30-58ab-4ff4-bbb2-95e4c8652203', '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093109', 2, null),
    ('1d3c6f30-58ab-4ff4-bbb2-95e4c8652204', '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093110', 1, null),
    ('1d3c6f30-58ab-4ff4-bbb2-95e4c8652204', '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093111', 2, null),
    ('1d3c6f30-58ab-4ff4-bbb2-95e4c8652205', '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093106', 1, 1150),
    ('1d3c6f30-58ab-4ff4-bbb2-95e4c8652205', '4f8aa1fd-d1a0-4ef1-a3c7-dccdb1093107', 2, 850)
on conflict (menu_section_id, menu_item_id) do update
set sort_order = excluded.sort_order,
    price_cents_override = excluded.price_cents_override;
