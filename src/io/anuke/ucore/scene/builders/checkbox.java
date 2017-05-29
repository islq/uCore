package io.anuke.ucore.scene.builders;

import io.anuke.ucore.scene.ui.CheckBox;
import io.anuke.ucore.scene.utils.function.CheckListenable;

public class checkbox extends builder<checkbox, CheckBox>{
	
	public checkbox(String text){
		this(text, null);
	}
	
	public checkbox(String text, CheckListenable listener){
		element = new CheckBox(text);
		if(listener != null)
		element.changed(()->{
			listener.listen(element.isChecked());
		});
		
		cell = context().add(element);
	}
	
	public checkbox changed(CheckListenable listener){
		element.changed(()->{
			listener.listen(element.isChecked());
		});
		return this;
	}
}
