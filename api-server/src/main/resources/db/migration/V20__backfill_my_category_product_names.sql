UPDATE categories
SET name_my = CASE slug
    WHEN 'men' THEN 'အမျိုးသား'
    WHEN 'women' THEN 'အမျိုးသမီး'
    WHEN 'bag' THEN 'အတွဲလိုက်ဝတ်စုံ'
    WHEN 'shirts' THEN 'ရှပ်အင်္ကျီ'
    WHEN 'pants' THEN 'ဘောင်းဘီရှည်'
    WHEN 'dresses' THEN 'ဝတ်စုံ'
    WHEN 'shoes' THEN 'ဖိနပ်'
    WHEN 't-shirt' THEN 'တီရှပ်'
    WHEN 'jackets' THEN 'အင်္ကျီအပေါ်ထပ်'
    WHEN 'jeans' THEN 'ဂျင်းဘောင်းဘီ'
    WHEN 'hoodies' THEN 'ဟူဒီ'
    WHEN 'shorts' THEN 'ဘောင်းဘီတို'
    WHEN 'blouses' THEN 'အမျိုးသမီးအင်္ကျီ'
    WHEN 'skirts' THEN 'စကပ်'
    WHEN 'women-tshirts' THEN 'အမျိုးသမီး တီရှပ်'
    WHEN 'cardigans' THEN 'ကာဒီဂန်'
    WHEN 'sneakers' THEN 'စနီကာ ဖိနပ်'
    WHEN 'boots' THEN 'ဘွတ်ဖိနပ်'
    WHEN 'sandals' THEN 'စန်ဒယ်'
    WHEN 'loafers' THEN 'လိုဖာဖိနပ်'
    WHEN 'couple-tshirts' THEN 'အတွဲတီရှပ်'
    WHEN 'couple-hoodies' THEN 'အတွဲဟူဒီ'
    WHEN 'couple-pajamas' THEN 'အတွဲအိပ်ဝတ်စုံ'
    WHEN 'couple-accessories' THEN 'အတွဲအသုံးအဆောင်'
    ELSE COALESCE(name_my, name_vi, name_en, name)
END;

UPDATE products
SET
    name_my = CASE slug
        WHEN 'cotton-t-shirt' THEN 'ကော်တွန် တီရှပ်'
        WHEN 'jeans' THEN 'ဂျင်းဘောင်းဘီ'
        WHEN 'summer-dress' THEN 'နွေရာသီဝတ်စုံ'
        WHEN 'sneakerssss' THEN 'အားကစားဖိနပ်'
        WHEN 'test' THEN 'စမ်းသပ်ပစ္စည်း'
        ELSE COALESCE(name_my, name_vi, name_en, name)
    END,
    description_my = CASE slug
        WHEN 'cotton-t-shirt' THEN 'သက်တောင့်သက်သာရှိသော ကော်တွန် တီရှပ်'
        WHEN 'jeans' THEN 'အပြာရောင် ဒီနင်မ် ဂျင်းဘောင်းဘီ'
        WHEN 'summer-dress' THEN 'ပေါ့ပါးသည့် နွေရာသီဝတ်စုံ'
        WHEN 'sneakerssss' THEN 'ခြေလှမ်းပေါ့သော အားကစားဖိနပ်'
        WHEN 'test' THEN 'စမ်းသပ်ဖော်ပြချက်'
        ELSE COALESCE(description_my, description_vi, description_en, description)
    END
WHERE is_deleted = FALSE;
