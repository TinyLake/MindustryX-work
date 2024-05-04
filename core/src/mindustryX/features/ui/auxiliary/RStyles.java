package mindustryX.features.ui.auxiliary;

import arc.graphics.*;
import arc.scene.style.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.TextButton.*;
import mindustry.gen.*;
import mindustry.ui.*;

import static mindustry.gen.Tex.underlineWhite;
import static mindustry.ui.Styles.*;

//move from mindustry.arcModule.ui.RStyles
public class RStyles{
    public static Drawable black1;

    public static TextButtonStyle
    clearLineNonet,
    clearLineNoneTogglet;

    public static ImageButtonStyle
    clearAccentNonei,
    clearAccentNoneTogglei,
    clearLineNonei,
    clearLineNoneTogglei;

    public static void load(){
        var whiteuir = (TextureRegionDrawable)Tex.whiteui;

        black1 = whiteuir.tint(0f, 0f, 0f, 0.1f);

        clearLineNonet = new TextButtonStyle(){{
            font = Fonts.def;
            fontColor = Color.white;
            disabled = black;
            disabledFontColor = Color.gray;
            up = none;
            over = accentDrawable;
            down = underlineWhite;
        }};

        clearLineNoneTogglet = new TextButtonStyle(fullTogglet){{
            up = none;
            over = accentDrawable;
            down = underlineWhite;
            checked = underlineWhite;
            disabledFontColor = Color.white;
        }};

        clearAccentNonei = new ImageButtonStyle(clearNonei){{
            up = none;
            over = black3;
            down = none;
        }};

        clearAccentNoneTogglei = new ImageButtonStyle(clearAccentNonei){{
            checked = accentDrawable;
        }};

        clearLineNonei = new ImageButtonStyle(clearNonei){{
            up = none;
            over = accentDrawable;
            down = none;
        }};

        clearLineNoneTogglei = new ImageButtonStyle(clearLineNonei){{
            checked = underlineWhite;
        }};
    }

}
