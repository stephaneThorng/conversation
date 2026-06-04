-- ============================================================
-- DATALOAD : Chez Alfredo
-- ============================================================
-- ------------------------------------------------------------
-- REFERENTIALS — MENU ITEM CATEGORIES
-- ------------------------------------------------------------
insert into restaurant_menu_item_category (code, name_translations, active) values
    ('appetizer',       '{"en":"Appetizer","fr":"Amuse-bouche"}',              true),
    ('starter',         '{"en":"Starter","fr":"Entree"}',                      true),
    ('soup',            '{"en":"Soup","fr":"Soupe"}',                          true),
    ('salad',           '{"en":"Salad","fr":"Salade"}',                        true),
    ('pasta',           '{"en":"Pasta","fr":"Pates"}',                         true),
    ('risotto',         '{"en":"Risotto","fr":"Risotto"}',                     true),
    ('main_course',     '{"en":"Main Course","fr":"Plat principal"}',          true),
    ('fish',            '{"en":"Fish","fr":"Poisson"}',                        true),
    ('meat',            '{"en":"Meat","fr":"Viande"}',                         true),
    ('vegetarian',      '{"en":"Vegetarian","fr":"Vegetarien"}',               true),
    ('cheese',          '{"en":"Cheese Board","fr":"Plateau de fromages"}',    true),
    ('dessert',         '{"en":"Dessert","fr":"Dessert"}',                     true),
    ('bread',           '{"en":"Bread & Bakery","fr":"Pain et viennoiserie"}', true),
    ('drink_soft',      '{"en":"Soft Drink","fr":"Boisson sans alcool"}',      true),
    ('drink_wine',      '{"en":"Wine","fr":"Vin"}',                            true),
    ('drink_cocktail',  '{"en":"Cocktail","fr":"Cocktail"}',                   true),
    ('drink_hot',       '{"en":"Hot Drink","fr":"Boisson chaude"}',            true),
    ('italian',         '{"en":"Italian","fr":"Italien"}',                     true),
    ('french',          '{"en":"French","fr":"Francais"}',                     true),
    ('grilled',         '{"en":"Grilled","fr":"Grille"}',                      true),
    ('seasonal',        '{"en":"Seasonal","fr":"De saison"}',                  true);
-- ------------------------------------------------------------
-- REFERENTIALS — ALLERGENS (14 EU major allergens + extras)
-- ------------------------------------------------------------
insert into restaurant_allergen (code, name_translations, active) values
    ('gluten',      '{"en":"Gluten (wheat, rye, barley, oat)","fr":"Gluten (ble, seigle, orge, avoine)"}',                                                                                            true),
    ('crustaceans', '{"en":"Crustaceans","fr":"Crustaces"}',                                                                                                                                          true),
    ('eggs',        '{"en":"Eggs","fr":"Oeufs"}',                                                                                                                                                     true),
    ('fish',        '{"en":"Fish","fr":"Poisson"}',                                                                                                                                                   true),
    ('peanuts',     '{"en":"Peanuts","fr":"Arachides"}',                                                                                                                                              true),
    ('soy',         '{"en":"Soy","fr":"Soja"}',                                                                                                                                                       true),
    ('dairy',       '{"en":"Milk & Dairy","fr":"Lait et produits laitiers"}',                                                                                                                         true),
    ('tree_nuts',   '{"en":"Tree Nuts (almond, hazelnut, walnut, cashew, pecan, pistachio, macadamia)","fr":"Fruits a coque (amande, noisette, noix, noix de cajou, pecan, pistache, macadamia)"}',   true),
    ('celery',      '{"en":"Celery","fr":"Celeri"}',                                                                                                                                                  true),
    ('mustard',     '{"en":"Mustard","fr":"Moutarde"}',                                                                                                                                               true),
    ('sesame',      '{"en":"Sesame","fr":"Sesame"}',                                                                                                                                                  true),
    ('sulphites',   '{"en":"Sulphur Dioxide & Sulphites","fr":"Dioxyde de soufre et sulfites"}',                                                                                                      true),
    ('lupin',       '{"en":"Lupin","fr":"Lupin"}',                                                                                                                                                    true),
    ('molluscs',    '{"en":"Molluscs","fr":"Mollusques"}',                                                                                                                                            true),
    ('garlic',      '{"en":"Garlic","fr":"Ail"}',                                                                                                                                                     true),
    ('onion',       '{"en":"Onion","fr":"Oignon"}',                                                                                                                                                   true),
    ('alcohol',     '{"en":"Alcohol","fr":"Alcool"}',                                                                                                                                                 true),
    ('corn',        '{"en":"Corn / Maize","fr":"Mais"}',                                                                                                                                              true);
-- ------------------------------------------------------------
-- REFERENTIALS — DIETARY RESTRICTIONS
-- ------------------------------------------------------------
insert into restaurant_dietary_restriction (code, name_translations, active) values
    ('vegan',               '{"en":"Vegan","fr":"Vegan"}',                                             true),
    ('vegetarian',          '{"en":"Vegetarian","fr":"Vegetarien"}',                                   true),
    ('pescatarian',         '{"en":"Pescatarian","fr":"Pescatarien"}',                                 true),
    ('gluten_free',         '{"en":"Gluten-Free","fr":"Sans gluten"}',                                 true),
    ('dairy_free',          '{"en":"Dairy-Free","fr":"Sans produits laitiers"}',                       true),
    ('egg_free',            '{"en":"Egg-Free","fr":"Sans oeuf"}',                                      true),
    ('nut_free',            '{"en":"Nut-Free","fr":"Sans fruits a coque"}',                            true),
    ('soy_free',            '{"en":"Soy-Free","fr":"Sans soja"}',                                      true),
    ('low_fodmap',          '{"en":"Low FODMAP","fr":"Faible en FODMAP"}',                             true),
    ('low_carb',            '{"en":"Low Carb","fr":"Faible en glucides"}',                             true),
    ('keto',                '{"en":"Ketogenic","fr":"Cetogene"}',                                      true),
    ('paleo',               '{"en":"Paleo","fr":"Paleo"}',                                             true),
    ('halal',               '{"en":"Halal","fr":"Halal"}',                                             true),
    ('kosher',              '{"en":"Kosher","fr":"Casher"}',                                           true),
    ('contains_alcohol',    '{"en":"Contains Alcohol","fr":"Contient de l''alcool"}',                  true),
    ('raw',                 '{"en":"Raw / Uncooked","fr":"Cru / non cuit"}',                           true),
    ('spicy',               '{"en":"Spicy","fr":"Epice"}',                                             true),
    ('diabetic_friendly',   '{"en":"Diabetic-Friendly","fr":"Convient aux diabetiques"}',              true);
-- ------------------------------------------------------------
-- ESTABLISHMENT
-- ------------------------------------------------------------
insert into restaurant_establishment (id, code, default_locale, name_translations, description_translations, active) values
    (
        '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201',
        'chez_alfredo',
        'fr',
        '{"en":"Chez Alfredo","fr":"Chez Alfredo"}',
        '{"en":"A refined Franco-Italian bistro nestled in the heart of the old town. Chef Alfredo blends Provencal warmth with Italian soul - honest ingredients, generous portions, and a wine list that tells a story.","fr":"Un bistrot franco-italien raffine loge au coeur de la vieille ville. Le chef Alfredo marie la chaleur provencale et l''ame italienne - produits honnetes, portions genereuses et une carte des vins qui raconte une histoire."}',
        true
    );
-- ------------------------------------------------------------
-- MENUS
-- ------------------------------------------------------------
insert into restaurant_menu (id, establishment_id, code, name_translations, description_translations, active, sort_order, price_cents, currency) values
    (
        'f1000000-0000-0000-0000-000000000001',
        '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201',
        'carte',
        '{"en":"A La Carte","fr":"La Carte"}',
        '{"en":"Our full menu, available for lunch and dinner.","fr":"Notre carte complete, disponible au dejeuner et au diner."}',
        true, 1, null, 'EUR'
    ),
    (
        'f1000000-0000-0000-0000-000000000002',
        '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201',
        'menu_dejeuner',
        '{"en":"Lunch Set Menu","fr":"Menu du Dejeuner"}',
        '{"en":"Two-course lunch menu - starter + main or main + dessert. Tuesday to Friday only.","fr":"Menu deux plats au choix - entree + plat ou plat + dessert. Du mardi au vendredi uniquement."}',
        true, 2, 1800, 'EUR'
    ),
    (
        'f1000000-0000-0000-0000-000000000003',
        '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201',
        'menu_degustation',
        '{"en":"Tasting Menu","fr":"Menu Degustation"}',
        '{"en":"Six-course tasting experience curated by Chef Alfredo. Seasonal and subject to change.","fr":"Experience de degustation en six services concoctee par le Chef Alfredo. Saisonniere et susceptible de changer."}',
        true, 3, 6500, 'EUR'
    );
-- ------------------------------------------------------------
-- MENU SECTIONS
-- ------------------------------------------------------------
insert into restaurant_menu_section (id, menu_id, code, name_translations, description_translations, sort_order, active) values
    ('e1000000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000001', 'amuse_bouche',
        '{"en":"Amuse-Bouche","fr":"Amuse-Bouche"}',
        '{"en":"Small bites to awaken the palate.","fr":"Petites mises en bouche pour eveiller les papilles."}',
        1, true),
    ('e1000000-0000-0000-0000-000000000002', 'f1000000-0000-0000-0000-000000000001', 'entrees',
        '{"en":"Starters","fr":"Entrees"}',
        '{"en":"Fresh, seasonal starters to begin your meal.","fr":"Entrees fraiches et de saison pour commencer votre repas."}',
        2, true),
    ('e1000000-0000-0000-0000-000000000003', 'f1000000-0000-0000-0000-000000000001', 'pates_risotto',
        '{"en":"Pasta & Risotto","fr":"Pates et Risotto"}',
        '{"en":"House-made pasta and slow-cooked risotto.","fr":"Pates maison et risotto mijote."}',
        3, true),
    ('e1000000-0000-0000-0000-000000000004', 'f1000000-0000-0000-0000-000000000001', 'plats',
        '{"en":"Main Courses","fr":"Plats Principaux"}',
        '{"en":"Meat, fish and vegetarian mains.","fr":"Viandes, poissons et plats vegetariens."}',
        4, true),
    ('e1000000-0000-0000-0000-000000000005', 'f1000000-0000-0000-0000-000000000001', 'fromages',
        '{"en":"Cheese Board","fr":"Plateau de Fromages"}',
        '{"en":"Selection of refined French and Italian cheeses.","fr":"Selection de fromages fins francais et italiens."}',
        5, true),
    ('e1000000-0000-0000-0000-000000000006', 'f1000000-0000-0000-0000-000000000001', 'desserts',
        '{"en":"Desserts","fr":"Desserts"}',
        '{"en":"House-made desserts and sweet finishes.","fr":"Desserts maison et douceurs de fin de repas."}',
        6, true),
    ('e1000000-0000-0000-0000-000000000007', 'f1000000-0000-0000-0000-000000000001', 'vins',
        '{"en":"Wines by the Glass","fr":"Vins au Verre"}',
        '{"en":"Carefully selected wines available by the glass.","fr":"Vins soigneusement selectionnes servis au verre."}',
        7, true),
    ('e1000000-0000-0000-0000-000000000008', 'f1000000-0000-0000-0000-000000000001', 'boissons',
        '{"en":"Drinks","fr":"Boissons"}',
        '{"en":"Soft drinks, hot drinks and cocktails.","fr":"Boissons sans alcool, boissons chaudes et cocktails."}',
        8, true),
    ('e1000000-0000-0000-0000-000000000009', 'f1000000-0000-0000-0000-000000000002', 'dejeuner_entrees',
        '{"en":"Starters (Lunch)","fr":"Entrees (Dejeuner)"}',
        '{"en":"Choose your starter.","fr":"Choisissez votre entree."}',
        1, true),
    ('e1000000-0000-0000-0000-000000000010', 'f1000000-0000-0000-0000-000000000002', 'dejeuner_plats',
        '{"en":"Mains (Lunch)","fr":"Plats (Dejeuner)"}',
        '{"en":"Choose your main course.","fr":"Choisissez votre plat."}',
        2, true),
    ('e1000000-0000-0000-0000-000000000011', 'f1000000-0000-0000-0000-000000000002', 'dejeuner_desserts',
        '{"en":"Desserts (Lunch)","fr":"Desserts (Dejeuner)"}',
        '{"en":"Choose your dessert.","fr":"Choisissez votre dessert."}',
        3, true),
    ('e1000000-0000-0000-0000-000000000012', 'f1000000-0000-0000-0000-000000000003', 'degustation_sequence',
        '{"en":"Tasting Sequence","fr":"Sequence de Degustation"}',
        '{"en":"Six courses served in sequence.","fr":"Six services presentes en sequence."}',
        1, true);
-- ------------------------------------------------------------
-- MENU ITEMS
-- ------------------------------------------------------------
insert into restaurant_menu_item (
    id, establishment_id, code,
    name_translations, description_translations, ingredient_note_translations,
    price_cents, currency, category_codes, allergen_codes, dietary_restriction_codes, active
) values
    -- AMUSE-BOUCHE
    ('d1000000-0000-0000-0000-000000000001', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'gougeres_parmesan',
        '{"en":"Parmesan Gougeres","fr":"Gougeres au Parmesan"}',
        '{"en":"Light choux pastry puffs filled with aged Parmesan. Two pieces.","fr":"Legeres choux farcies au parmesan affine. Deux pieces."}',
        '{"en":"Choux pastry (flour, butter, eggs), Parmesan, nutmeg.","fr":"Pate a choux (farine, beurre, oeufs), parmesan, noix de muscade."}',
        400, 'EUR', '{"appetizer","french"}', '{"gluten","eggs","dairy"}', '{}', true),
    ('d1000000-0000-0000-0000-000000000002', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'bruschetta_tomate_basilic',
        '{"en":"Bruschetta with Heirloom Tomato & Basil","fr":"Bruschetta tomate ancienne et basilic"}',
        '{"en":"Grilled sourdough, slow-roasted heirloom tomatoes, fresh basil, aged balsamic.","fr":"Pain au levain grille, tomates anciennes rotissees lentement, basilic frais, balsamique age."}',
        '{"en":"Sourdough bread, heirloom tomatoes, basil, garlic, extra-virgin olive oil, balsamic vinegar.","fr":"Pain au levain, tomates anciennes, basilic, ail, huile d''olive vierge extra, vinaigre balsamique."}',
        500, 'EUR', '{"appetizer","italian"}', '{"gluten","sulphites"}', '{"vegan"}', true),
    -- STARTERS
    ('d1000000-0000-0000-0000-000000000003', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'burrata_prosciutto',
        '{"en":"Burrata & Prosciutto di Parma","fr":"Burrata et Jambon de Parme"}',
        '{"en":"Creamy burrata, hand-sliced Prosciutto di Parma, roasted cherry tomatoes, basil oil.","fr":"Burrata creme, jambon de Parme tranche a la main, tomates cerises rotissees, huile de basilic."}',
        '{"en":"Burrata (cow milk), Prosciutto di Parma (pork), cherry tomatoes, basil, olive oil.","fr":"Burrata (lait de vache), jambon de Parme (porc), tomates cerises, basilic, huile d''olive."}',
        1400, 'EUR', '{"starter","italian"}', '{"dairy"}', '{"gluten_free"}', true),
    ('d1000000-0000-0000-0000-000000000004', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'soupe_oignon_gratinee',
        '{"en":"French Onion Soup Gratinee","fr":"Soupe a l''Oignon Gratinee"}',
        '{"en":"Slow-caramelised onion broth, croute of country bread, melted Gruyere gratin.","fr":"Bouillon d''oignons caramelises lentement, croute de pain de campagne, gratin de gruyere fondu."}',
        '{"en":"Onion, beef stock, country bread, Gruyere, butter, thyme, bay leaf.","fr":"Oignon, bouillon de boeuf, pain de campagne, gruyere, beurre, thym, laurier."}',
        1100, 'EUR', '{"starter","soup","french"}', '{"gluten","dairy"}', '{}', true),
    ('d1000000-0000-0000-0000-000000000005', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'carpaccio_boeuf',
        '{"en":"Beef Carpaccio","fr":"Carpaccio de Boeuf"}',
        '{"en":"Thinly sliced raw beef fillet, rocket, aged Parmesan shavings, capers, lemon zest, truffle oil.","fr":"Filet de boeuf cru tranche finement, roquette, copeaux de parmesan affine, capres, zeste de citron, huile de truffe."}',
        '{"en":"Raw beef fillet, rocket, Parmesan, capers, lemon, truffle oil, mustard.","fr":"Filet de boeuf cru, roquette, parmesan, capres, citron, huile de truffe, moutarde."}',
        1600, 'EUR', '{"starter","italian","meat"}', '{"dairy","mustard","sulphites"}', '{"gluten_free","raw"}', true),
    ('d1000000-0000-0000-0000-000000000006', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'salade_chevre_chaud',
        '{"en":"Warm Goat Cheese Salad","fr":"Salade de Chevre Chaud"}',
        '{"en":"Mixed greens, warm breaded goat cheese medallion, honey-walnut dressing, dried cranberries.","fr":"Mesclun, medaillon de chevre chaud pane, vinaigrette miel-noix, cranberries sechees."}',
        '{"en":"Mixed greens, goat cheese, breadcrumbs (gluten), walnuts, honey, cider vinegar, cranberries.","fr":"Mesclun, fromage de chevre, chapelure (gluten), noix, miel, vinaigre de cidre, cranberries."}',
        1200, 'EUR', '{"starter","salad","french","vegetarian"}', '{"gluten","dairy","tree_nuts","eggs"}', '{"vegetarian"}', true),
    -- PASTA & RISOTTO
    ('d1000000-0000-0000-0000-000000000007', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'tagliatelle_truffe_noire',
        '{"en":"Black Truffle Tagliatelle","fr":"Tagliatelles a la Truffe Noire"}',
        '{"en":"Fresh egg tagliatelle, shaved black truffle, brown butter, aged Parmesan, chives.","fr":"Tagliatelles aux oeufs fraiches, copeaux de truffe noire, beurre noisette, parmesan affine, ciboulette."}',
        '{"en":"Durum wheat semolina, eggs, black truffle, butter, Parmesan, chives.","fr":"Semolina de ble dur, oeufs, truffe noire, beurre, parmesan, ciboulette."}',
        2800, 'EUR', '{"pasta","italian","seasonal"}', '{"gluten","eggs","dairy"}', '{"vegetarian"}', true),
    ('d1000000-0000-0000-0000-000000000008', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'pappardelle_sanglier',
        '{"en":"Pappardelle with Wild Boar Ragu","fr":"Pappardelle au Ragu de Sanglier"}',
        '{"en":"Wide house-made pappardelle, slow-braised wild boar ragu, rosemary, pecorino.","fr":"Larges pappardelle maison, ragu de sanglier braise lentement, romarin, pecorino."}',
        '{"en":"Durum wheat semolina, eggs, wild boar, red wine, carrot, celery, onion, rosemary, pecorino.","fr":"Semolina de ble dur, oeufs, sanglier, vin rouge, carotte, celeri, oignon, romarin, pecorino."}',
        2400, 'EUR', '{"pasta","italian","meat"}', '{"gluten","eggs","dairy","celery","sulphites"}', '{}', true),
    ('d1000000-0000-0000-0000-000000000009', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'risotto_fruits_mer',
        '{"en":"Seafood Risotto","fr":"Risotto aux Fruits de Mer"}',
        '{"en":"Carnaroli rice, clams, mussels, squid, king prawn, saffron broth, white wine, lemon.","fr":"Riz carnaroli, palourdes, moules, encornet, gambas, bouillon de safran, vin blanc, citron."}',
        '{"en":"Carnaroli rice, clams, mussels, squid, king prawn, white wine, shallot, saffron, olive oil, parsley.","fr":"Riz carnaroli, palourdes, moules, encornet, gambas, vin blanc, echalote, safran, huile d''olive, persil."}',
        2600, 'EUR', '{"risotto","italian","fish"}', '{"crustaceans","molluscs","fish","sulphites"}', '{"gluten_free","dairy_free"}', true),
    ('d1000000-0000-0000-0000-000000000010', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'risotto_champignons_sauvages',
        '{"en":"Wild Mushroom Risotto","fr":"Risotto aux Champignons Sauvages"}',
        '{"en":"Carnaroli rice, porcini, chanterelles, oyster mushrooms, mascarpone, thyme, truffle oil.","fr":"Riz carnaroli, cepes, girolles, pleurotes, mascarpone, thym, huile de truffe."}',
        '{"en":"Carnaroli rice, mixed wild mushrooms, mascarpone, Parmesan, butter, white wine, thyme, truffle oil.","fr":"Riz carnaroli, champignons sauvages melanges, mascarpone, parmesan, beurre, vin blanc, thym, huile de truffe."}',
        2000, 'EUR', '{"risotto","italian","vegetarian","seasonal"}', '{"dairy","sulphites"}', '{"vegetarian","gluten_free"}', true),
    -- MAIN COURSES
    ('d1000000-0000-0000-0000-000000000011', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'magret_canard',
        '{"en":"Duck Breast with Cherry Jus","fr":"Magret de Canard au Jus de Cerises"}',
        '{"en":"Pan-seared duck breast, cherry and port reduction, gratin dauphinois, wilted spinach.","fr":"Magret de canard poele, reduction cerises et porto, gratin dauphinois, epinards etouves."}',
        '{"en":"Duck breast, cherries, port wine, duck jus, cream, potatoes, garlic, nutmeg, spinach.","fr":"Magret de canard, cerises, porto, jus de canard, creme, pommes de terre, ail, muscade, epinards."}',
        3200, 'EUR', '{"main_course","meat","french"}', '{"dairy","sulphites"}', '{"gluten_free"}', true),
    ('d1000000-0000-0000-0000-000000000012', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'cote_veau_milanese',
        '{"en":"Veal Milanese","fr":"Cote de Veau Milanaise"}',
        '{"en":"Breaded veal cutlet, slow-cooked tomato and herb sauce, saffron risotto.","fr":"Cote de veau panee, sauce tomate aux herbes mijotee, risotto au safran."}',
        '{"en":"Veal, breadcrumbs (gluten), eggs, Parmesan, tomato, basil, white wine, saffron, Carnaroli rice, butter.","fr":"Veau, chapelure (gluten), oeufs, parmesan, tomate, basilic, vin blanc, safran, riz carnaroli, beurre."}',
        3400, 'EUR', '{"main_course","meat","italian"}', '{"gluten","eggs","dairy","sulphites"}', '{}', true),
    ('d1000000-0000-0000-0000-000000000013', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'loup_bar_fenouil',
        '{"en":"Sea Bass with Fennel & Pastis","fr":"Loup de Mer au Fenouil et Pastis"}',
        '{"en":"Whole grilled sea bass fillet, braised fennel, pastis cream sauce, pommes vapeur.","fr":"Filet de loup de mer entier grille, fenouil braise, creme au pastis, pommes vapeur."}',
        '{"en":"Sea bass fillet, fennel, pastis, cream, shallot, dill, potatoes.","fr":"Filet de loup de mer, fenouil, pastis, creme, echalote, aneth, pommes de terre."}',
        3000, 'EUR', '{"main_course","fish","french","grilled"}', '{"fish","dairy","sulphites","alcohol"}', '{"gluten_free"}', true),
    ('d1000000-0000-0000-0000-000000000014', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'saltimbocca_alla_romana',
        '{"en":"Saltimbocca alla Romana","fr":"Saltimbocca a la Romaine"}',
        '{"en":"Veal escalope, Parma ham, fresh sage, white wine pan jus, roasted artichokes.","fr":"Escalope de veau, jambon de Parme, sauge fraiche, jus de veau au vin blanc, artichauts roties."}',
        '{"en":"Veal escalope, Prosciutto di Parma, sage, butter, white wine, artichokes, lemon.","fr":"Escalope de veau, jambon de Parme, sauge, beurre, vin blanc, artichauts, citron."}',
        3100, 'EUR', '{"main_course","meat","italian"}', '{"dairy","sulphites"}', '{"gluten_free"}', true),
    ('d1000000-0000-0000-0000-000000000015', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'aubergine_parmigiana',
        '{"en":"Aubergine Parmigiana","fr":"Parmigiana d''Aubergine"}',
        '{"en":"Layers of roasted aubergine, San Marzano tomato sauce, fresh mozzarella, aged Parmesan, basil.","fr":"Couches d''aubergine rotie, sauce tomate San Marzano, mozzarella fraiche, parmesan affine, basilic."}',
        '{"en":"Aubergine, San Marzano tomato, mozzarella, Parmesan, basil, olive oil, garlic.","fr":"Aubergine, tomate San Marzano, mozzarella, parmesan, basilic, huile d''olive, ail."}',
        1900, 'EUR', '{"main_course","vegetarian","italian"}', '{"dairy"}', '{"vegetarian","gluten_free"}', true),
    -- CHEESE
    ('d1000000-0000-0000-0000-000000000016', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'plateau_fromages',
        '{"en":"Cheese Board - Selection of Five","fr":"Plateau de Cinq Fromages Affines"}',
        '{"en":"Daily selection of five refined cheeses - French and Italian. Served with fig jam, walnuts, and country bread.","fr":"Selection quotidienne de cinq fromages affines - francais et italiens. Servi avec confiture de figues, noix et pain de campagne."}',
        '{"en":"Cow, sheep and goat cheeses (varies daily), fig jam, walnuts, sourdough bread.","fr":"Fromages vache, brebis et chevre (variable selon le jour), confiture de figues, noix, pain au levain."}',
        1800, 'EUR', '{"cheese","french","italian"}', '{"dairy","gluten","tree_nuts","sulphites"}', '{"vegetarian"}', true),
    -- DESSERTS
    ('d1000000-0000-0000-0000-000000000017', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'tiramisu_classique',
        '{"en":"Classic Tiramisu","fr":"Tiramisu Classique"}',
        '{"en":"Espresso-soaked savoiardi biscuits, mascarpone cream, dark cocoa. Our signature.","fr":"Biscuits savoiardi imbibes d''espresso, creme de mascarpone, cacao noir. Notre signature."}',
        '{"en":"Mascarpone, eggs, caster sugar, espresso, savoiardi (gluten, eggs), Marsala wine, cocoa.","fr":"Mascarpone, oeufs, sucre fin, espresso, savoiardi (gluten, oeufs), vin de Marsala, cacao."}',
        1000, 'EUR', '{"dessert","italian"}', '{"gluten","eggs","dairy","sulphites","alcohol"}', '{"vegetarian"}', true),
    ('d1000000-0000-0000-0000-000000000018', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'creme_brulee_vanille',
        '{"en":"Vanilla Creme Brulee","fr":"Creme Brulee a la Vanille de Madagascar"}',
        '{"en":"Classic French creme brulee perfumed with Madagascar vanilla, caramelised sugar crust.","fr":"Creme brulee francaise classique parfumee a la vanille de Madagascar, croute de sucre caramelisee."}',
        '{"en":"Cream, egg yolks, caster sugar, Madagascar vanilla pod.","fr":"Creme, jaunes d''oeufs, sucre fin, gousse de vanille de Madagascar."}',
        900, 'EUR', '{"dessert","french"}', '{"eggs","dairy"}', '{"vegetarian","gluten_free"}', true),
    ('d1000000-0000-0000-0000-000000000019', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'panna_cotta_fruits_rouges',
        '{"en":"Panna Cotta with Berry Coulis","fr":"Panna Cotta au Coulis de Fruits Rouges"}',
        '{"en":"Silky vanilla panna cotta, fresh seasonal berry coulis, mint.","fr":"Panna cotta vanillee soyeuse, coulis de fruits rouges frais de saison, menthe."}',
        '{"en":"Cream, sugar, vanilla, gelatine, mixed berries (strawberry, raspberry, blueberry).","fr":"Creme, sucre, vanille, gelatine, fruits rouges melanges (fraise, framboise, myrtille)."}',
        800, 'EUR', '{"dessert","italian"}', '{"dairy"}', '{"vegetarian","gluten_free"}', true),
    ('d1000000-0000-0000-0000-000000000020', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'fondant_chocolat_noisette',
        '{"en":"Dark Chocolate & Hazelnut Fondant","fr":"Fondant Chocolat Noir et Noisette"}',
        '{"en":"Warm dark chocolate fondant with a molten hazelnut praline heart, vanilla ice cream.","fr":"Fondant au chocolat noir chaud au coeur coulant de praline noisette, glace vanille."}',
        '{"en":"Dark chocolate (70%), butter, eggs, flour, caster sugar, hazelnut praline, vanilla ice cream.","fr":"Chocolat noir (70%), beurre, oeufs, farine, sucre fin, praline noisette, glace vanille."}',
        1100, 'EUR', '{"dessert","french"}', '{"gluten","eggs","dairy","tree_nuts"}', '{"vegetarian"}', true),
    ('d1000000-0000-0000-0000-000000000021', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'tarte_tatin_pomme',
        '{"en":"Apple Tarte Tatin","fr":"Tarte Tatin aux Pommes"}',
        '{"en":"Upside-down caramelised apple tart, buttery puff pastry, creme fraiche.","fr":"Tarte aux pommes caramelisees renversee, pate feuilletee au beurre, creme fraiche."}',
        '{"en":"Apple, butter, caster sugar, puff pastry (gluten, butter), creme fraiche.","fr":"Pomme, beurre, sucre fin, pate feuilletee (gluten, beurre), creme fraiche."}',
        900, 'EUR', '{"dessert","french","seasonal"}', '{"gluten","dairy"}', '{"vegetarian"}', true),
    -- WINES BY THE GLASS
    ('d1000000-0000-0000-0000-000000000022', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'vin_blanc_picpoul',
        '{"en":"Picpoul de Pinet - White (15 cl)","fr":"Picpoul de Pinet - Blanc (15 cl)"}',
        '{"en":"Crisp, mineral Languedoc white with citrus and green apple notes.","fr":"Blanc du Languedoc vif et mineral, notes d''agrumes et de pomme verte."}',
        '{"en":"Picpoul de Pinet (sulphites).","fr":"Picpoul de Pinet (sulfites)."}',
        700, 'EUR', '{"drink_wine","french"}', '{"sulphites","alcohol"}', '{"vegan","contains_alcohol"}', true),
    ('d1000000-0000-0000-0000-000000000023', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'vin_rouge_barolo',
        '{"en":"Barolo DOCG - Red (15 cl)","fr":"Barolo DOCG - Rouge (15 cl)"}',
        '{"en":"Full-bodied Nebbiolo from Piedmont, notes of cherry, tar and rose.","fr":"Nebbiolo corse du Piemont, notes de cerise, goudron et rose."}',
        '{"en":"Barolo DOCG, Nebbiolo grape (sulphites).","fr":"Barolo DOCG, raisin Nebbiolo (sulfites)."}',
        1200, 'EUR', '{"drink_wine","italian"}', '{"sulphites","alcohol"}', '{"vegan","contains_alcohol"}', true),
    ('d1000000-0000-0000-0000-000000000024', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'champagne_brut',
        '{"en":"Champagne Brut NV (12 cl)","fr":"Champagne Brut NV (12 cl)"}',
        '{"en":"Elegant house Champagne, fine bubbles, brioche and pear notes.","fr":"Champagne de maison elegant, fines bulles, notes de brioche et de poire."}',
        '{"en":"Champagne Brut NV (sulphites).","fr":"Champagne Brut NV (sulfites)."}',
        1400, 'EUR', '{"drink_wine","french"}', '{"sulphites","alcohol"}', '{"vegan","contains_alcohol"}', true),
    -- DRINKS
    ('d1000000-0000-0000-0000-000000000025', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'limonade_maison',
        '{"en":"House Lemonade","fr":"Limonade Maison"}',
        '{"en":"Fresh-squeezed lemon, cane sugar syrup, sparkling water, fresh mint.","fr":"Citron presse frais, sirop de sucre de canne, eau gazeuse, menthe fraiche."}',
        '{"en":"Lemon, cane sugar, sparkling water, mint.","fr":"Citron, sucre de canne, eau gazeuse, menthe."}',
        500, 'EUR', '{"drink_soft"}', '{}', '{"vegan","gluten_free","dairy_free"}', true),
    ('d1000000-0000-0000-0000-000000000026', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'espresso_double',
        '{"en":"Double Espresso","fr":"Cafe Espresso Double"}',
        '{"en":"Two shots of our house-blend espresso, served with a small piece of dark chocolate.","fr":"Double ristretto de notre blend maison, servi avec un carre de chocolat noir."}',
        '{"en":"Arabica espresso blend, dark chocolate square (may contain dairy).","fr":"Blend espresso arabica, carre de chocolat noir (peut contenir des laitages)."}',
        350, 'EUR', '{"drink_hot"}', '{}', '{"vegan","gluten_free"}', true),
    ('d1000000-0000-0000-0000-000000000027', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'cocktail_aperol_spritz',
        '{"en":"Aperol Spritz","fr":"Aperol Spritz"}',
        '{"en":"Aperol, Prosecco, splash of soda, orange slice.","fr":"Aperol, Prosecco, trait de soda, rondelle d''orange."}',
        '{"en":"Aperol (sulphites, alcohol), Prosecco (sulphites, alcohol), soda, orange.","fr":"Aperol (sulfites, alcool), Prosecco (sulfites, alcool), soda, orange."}',
        900, 'EUR', '{"drink_cocktail","italian"}', '{"sulphites","alcohol"}', '{"vegan","contains_alcohol"}', true),
    ('d1000000-0000-0000-0000-000000000028', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'eau_gazeuse',
        '{"en":"Sparkling Mineral Water (75 cl)","fr":"Eau Minerale Gazeuse (75 cl)"}',
        '{"en":"Chilled sparkling mineral water.","fr":"Eau minerale gazeuse fraiche."}',
        '{"en":"Sparkling mineral water.","fr":"Eau minerale gazeuse."}',
        400, 'EUR', '{"drink_soft"}', '{}', '{"vegan","gluten_free","dairy_free"}', true);
-- ------------------------------------------------------------
-- MENU SECTION ITEM MAP
-- ------------------------------------------------------------
insert into restaurant_menu_section_item_map (menu_section_id, menu_item_id, sort_order, price_cents_override) values
    -- La Carte: Amuse-Bouche
    ('e1000000-0000-0000-0000-000000000001', 'd1000000-0000-0000-0000-000000000001', 1, null),
    ('e1000000-0000-0000-0000-000000000001', 'd1000000-0000-0000-0000-000000000002', 2, null),
    -- La Carte: Starters
    ('e1000000-0000-0000-0000-000000000002', 'd1000000-0000-0000-0000-000000000003', 1, null),
    ('e1000000-0000-0000-0000-000000000002', 'd1000000-0000-0000-0000-000000000004', 2, null),
    ('e1000000-0000-0000-0000-000000000002', 'd1000000-0000-0000-0000-000000000005', 3, null),
    ('e1000000-0000-0000-0000-000000000002', 'd1000000-0000-0000-0000-000000000006', 4, null),
    -- La Carte: Pasta & Risotto
    ('e1000000-0000-0000-0000-000000000003', 'd1000000-0000-0000-0000-000000000007', 1, null),
    ('e1000000-0000-0000-0000-000000000003', 'd1000000-0000-0000-0000-000000000008', 2, null),
    ('e1000000-0000-0000-0000-000000000003', 'd1000000-0000-0000-0000-000000000009', 3, null),
    ('e1000000-0000-0000-0000-000000000003', 'd1000000-0000-0000-0000-000000000010', 4, null),
    -- La Carte: Mains
    ('e1000000-0000-0000-0000-000000000004', 'd1000000-0000-0000-0000-000000000011', 1, null),
    ('e1000000-0000-0000-0000-000000000004', 'd1000000-0000-0000-0000-000000000012', 2, null),
    ('e1000000-0000-0000-0000-000000000004', 'd1000000-0000-0000-0000-000000000013', 3, null),
    ('e1000000-0000-0000-0000-000000000004', 'd1000000-0000-0000-0000-000000000014', 4, null),
    ('e1000000-0000-0000-0000-000000000004', 'd1000000-0000-0000-0000-000000000015', 5, null),
    -- La Carte: Cheese
    ('e1000000-0000-0000-0000-000000000005', 'd1000000-0000-0000-0000-000000000016', 1, null),
    -- La Carte: Desserts
    ('e1000000-0000-0000-0000-000000000006', 'd1000000-0000-0000-0000-000000000017', 1, null),
    ('e1000000-0000-0000-0000-000000000006', 'd1000000-0000-0000-0000-000000000018', 2, null),
    ('e1000000-0000-0000-0000-000000000006', 'd1000000-0000-0000-0000-000000000019', 3, null),
    ('e1000000-0000-0000-0000-000000000006', 'd1000000-0000-0000-0000-000000000020', 4, null),
    ('e1000000-0000-0000-0000-000000000006', 'd1000000-0000-0000-0000-000000000021', 5, null),
    -- La Carte: Wines
    ('e1000000-0000-0000-0000-000000000007', 'd1000000-0000-0000-0000-000000000022', 1, null),
    ('e1000000-0000-0000-0000-000000000007', 'd1000000-0000-0000-0000-000000000023', 2, null),
    ('e1000000-0000-0000-0000-000000000007', 'd1000000-0000-0000-0000-000000000024', 3, null),
    -- La Carte: Drinks
    ('e1000000-0000-0000-0000-000000000008', 'd1000000-0000-0000-0000-000000000025', 1, null),
    ('e1000000-0000-0000-0000-000000000008', 'd1000000-0000-0000-0000-000000000026', 2, null),
    ('e1000000-0000-0000-0000-000000000008', 'd1000000-0000-0000-0000-000000000027', 3, null),
    ('e1000000-0000-0000-0000-000000000008', 'd1000000-0000-0000-0000-000000000028', 4, null),
    -- Menu Dejeuner: Starters
    ('e1000000-0000-0000-0000-000000000009', 'd1000000-0000-0000-0000-000000000004', 1, null),
    ('e1000000-0000-0000-0000-000000000009', 'd1000000-0000-0000-0000-000000000006', 2, null),
    -- Menu Dejeuner: Mains
    ('e1000000-0000-0000-0000-000000000010', 'd1000000-0000-0000-0000-000000000010', 1, null),
    ('e1000000-0000-0000-0000-000000000010', 'd1000000-0000-0000-0000-000000000013', 2, null),
    ('e1000000-0000-0000-0000-000000000010', 'd1000000-0000-0000-0000-000000000015', 3, null),
    -- Menu Dejeuner: Desserts (inclus dans le menu — price_cents_override = 0)
    ('e1000000-0000-0000-0000-000000000011', 'd1000000-0000-0000-0000-000000000018', 1, 0),
    ('e1000000-0000-0000-0000-000000000011', 'd1000000-0000-0000-0000-000000000019', 2, 0),
    ('e1000000-0000-0000-0000-000000000011', 'd1000000-0000-0000-0000-000000000021', 3, 0),
    -- Menu Degustation: sequence complete (inclus — price_cents_override = 0)
    ('e1000000-0000-0000-0000-000000000012', 'd1000000-0000-0000-0000-000000000001', 1, 0),
    ('e1000000-0000-0000-0000-000000000012', 'd1000000-0000-0000-0000-000000000005', 2, 0),
    ('e1000000-0000-0000-0000-000000000012', 'd1000000-0000-0000-0000-000000000007', 3, 0),
    ('e1000000-0000-0000-0000-000000000012', 'd1000000-0000-0000-0000-000000000011', 4, 0),
    ('e1000000-0000-0000-0000-000000000012', 'd1000000-0000-0000-0000-000000000016', 5, 0),
    ('e1000000-0000-0000-0000-000000000012', 'd1000000-0000-0000-0000-000000000017', 6, 0);
-- ============================================================
-- RESERVATION CONFIG
-- Duration   : 120 min (fine dining pacing)
-- Merging    : enabled (max 3 tables enforced applicatively)
-- Advance    : up to 60 days
-- ============================================================
insert into restaurant_reservation_config (establishment_id, reservation_duration_minutes, allow_table_merging, max_advance_days) values
    ('7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 120, true, 60);
-- ============================================================
-- OPENING HOURS
-- Monday (1)    : closed -> no row
-- Tuesday (2)   : dinner only 19:00-22:30
-- Wednesday (3) : lunch 12:00-14:30 + dinner 19:00-22:30
-- Thursday (4)  : lunch 12:00-14:30 + dinner 19:00-22:30
-- Friday (5)    : lunch 12:00-14:30 + dinner 19:00-23:00
-- Saturday (6)  : lunch 12:00-15:00 + dinner 19:00-23:00
-- Sunday (7)    : brunch 11:30-15:00 (no dinner)
-- ============================================================
insert into restaurant_opening_hours (id, establishment_id, day_of_week, open_time, close_time, active) values
    ('a2000000-0000-0000-0000-000000000001', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 2, '19:00', '22:30', true),
    ('a2000000-0000-0000-0000-000000000002', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 3, '12:00', '14:30', true),
    ('a2000000-0000-0000-0000-000000000003', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 3, '19:00', '22:30', true),
    ('a2000000-0000-0000-0000-000000000004', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 4, '12:00', '14:30', true),
    ('a2000000-0000-0000-0000-000000000005', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 4, '19:00', '22:30', true),
    ('a2000000-0000-0000-0000-000000000006', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 5, '12:00', '14:30', true),
    ('a2000000-0000-0000-0000-000000000007', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 5, '19:00', '23:00', true),
    ('a2000000-0000-0000-0000-000000000008', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 6, '12:00', '15:00', true),
    ('a2000000-0000-0000-0000-000000000009', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 6, '19:00', '23:00', true),
    ('a2000000-0000-0000-0000-000000000010', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 7, '11:30', '15:00', true);
-- ============================================================
-- CLOSURES (exceptional closing dates)
-- ============================================================
insert into restaurant_closure (id, establishment_id, closure_date, reason, active) values
    ('c2000000-0000-0000-0000-000000000001', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', '2026-08-15', 'Fermeture estivale annuelle - Assomption',        true),
    ('c2000000-0000-0000-0000-000000000002', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', '2026-11-01', 'Toussaint - fermeture exceptionnelle',             true),
    ('c2000000-0000-0000-0000-000000000003', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', '2026-12-31', 'New Year''s Eve - Private event, fully booked',    true);
-- ============================================================
-- PHYSICAL TABLES
-- T01-T02 : 2 seats  (bar / couple tables)
-- T03-T06 : 4 seats  (standard tables)
-- T07-T08 : 6 seats  (large / family tables)
-- T09     : 8 seats  (private dining / long table)
-- ============================================================
insert into restaurant_table (id, establishment_id, table_number, seat_count, active) values
    ('b2000000-0000-0000-0000-000000000001', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'T01', 2, true),
    ('b2000000-0000-0000-0000-000000000002', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'T02', 2, true),
    ('b2000000-0000-0000-0000-000000000003', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'T03', 4, true),
    ('b2000000-0000-0000-0000-000000000004', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'T04', 4, true),
    ('b2000000-0000-0000-0000-000000000005', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'T05', 4, true),
    ('b2000000-0000-0000-0000-000000000006', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'T06', 4, true),
    ('b2000000-0000-0000-0000-000000000007', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'T07', 6, true),
    ('b2000000-0000-0000-0000-000000000008', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'T08', 6, true),
    ('b2000000-0000-0000-0000-000000000009', '7a1e3d5c-9b2f-4e8a-bc10-3f42aee7c201', 'T09', 8, true);
