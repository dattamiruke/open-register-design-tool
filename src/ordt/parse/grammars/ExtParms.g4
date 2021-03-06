// External parameter file grammar
//
// Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.

grammar ExtParms;

ext_parms_root
  :  ext_parm_defs
     EOF
  ;

ext_parm_defs
  : ( global_defs
    | rdl_in_defs
    | jspec_in_defs
    | rdl_out_defs
    | jspec_out_defs
    | systemverilog_out_defs
    | uvmregs_out_defs
    | reglist_out_defs
    | bench_out_defs
 //   | cppmod_out_defs
    | model_annotation
    )*
  ;

// ------------ global_defs
 global_defs
   : 'global'
     LBRACE
     (global_parm_assign)+
     RBRACE
   ;

 global_parm_assign
   : 'min_data_size' EQ NUM
   | 'base_address' EQ NUM
   | 'use_js_address_alignment' EQ bool
   | 'suppress_alignment_warnings' EQ bool
   | 'default_base_map_name' EQ STR
   | 'allow_unordered_addresses' EQ bool
   | 'debug_mode' EQ NUM
   ;

// ------------ rdl_in_defs
 rdl_in_defs
   : 'input' 'rdl'
     LBRACE
     (rdl_in_parm_assign)+
     RBRACE
   ;
   
 rdl_in_parm_assign
   : 'process_component' EQ STR
   | 'resolve_reg_category' EQ bool
   ;
   
// ------------ jspec_in_defs
 jspec_in_defs
   : 'input' 'jspec'
     LBRACE
     (jspec_in_parm_assign)+
     RBRACE
   ;
   
 jspec_in_parm_assign
   : 'process_typedef' EQ STR
   | 'root_regset_is_addrmap' EQ bool
   | 'root_is_external_decode' EQ bool // deprecate??
   | 'external_replication_threshold' EQ NUM
   ;
   
// ------------ rdl_out_defs
 rdl_out_defs
   : 'output' 'rdl'
     LBRACE
     (rdl_out_parm_assign)+
     RBRACE
   ;
   
 rdl_out_parm_assign
   : 'root_component_is_instanced' EQ bool
   | 'output_jspec_attributes' EQ bool
   | 'no_root_enum_defs' EQ bool
   ;

// ------------ jspec_out_defs
 jspec_out_defs
   : 'output' 'jspec'
     LBRACE
     (jspec_out_parm_assign)+
     RBRACE
   ;
   
 jspec_out_parm_assign
   : 'root_regset_is_instanced' EQ bool
   | 'external_decode_is_root' EQ bool // deprecate??
   | 'add_js_include' EQ STR
   ;
   
// ------------ systemverilog_out_defs
 systemverilog_out_defs
   : 'output' 'systemverilog'
     LBRACE
     (systemverilog_out_parm_assign)+
     RBRACE
   ;
   
 systemverilog_out_parm_assign
   : 'leaf_address_size' EQ NUM
   | 'root_has_leaf_interface' EQ bool 
   | 'root_decoder_interface' EQ ('default' | 'leaf' | 'serial8' | 'ring16') 
   | 'base_addr_is_parameter' EQ bool 
   | 'module_tag' EQ STR 
   | 'use_gated_logic_clock' EQ bool 
   | 'use_external_select' EQ bool 
   | 'block_select_mode' EQ ('internal' | 'external' | 'always') 
   | 'export_start_end' EQ bool 
   | 'always_generate_iwrap' EQ bool 
   | 'suppress_no_reset_warnings' EQ bool
   | 'generate_child_addrmaps' EQ bool
   | 'ring16_inter_node_delay' EQ NUM
   | 'bbv5_timeout_input' EQ bool
   | 'include_default_coverage' EQ bool
   | 'generate_external_regs' EQ bool   // also allowed in bench output
   ;
      
// ------------ uvmregs_out_defs
 uvmregs_out_defs
   : 'output' 'uvmregs'
     LBRACE
     (uvmregs_out_parm_assign)+
     RBRACE
   ;
   
 uvmregs_out_parm_assign
   : 'is_mem_threshold' EQ NUM
   | 'suppress_no_category_warnings' EQ bool
   | 'include_address_coverage' EQ bool
   | 'max_reg_coverage_bins' EQ NUM
   ;   
   
// ------------ reglist_out_defs
 reglist_out_defs
   : 'output' 'reglist'
     LBRACE
     (reglist_out_parm_assign)+
     RBRACE
   ;  
   
 reglist_out_parm_assign
   : 'display_external_regs' EQ bool
   | 'show_reg_type' EQ bool 
   | 'match_instance' EQ STR 
   | 'show_fields' EQ bool 
   ;
   
// ------------ bench_out_defs
 bench_out_defs
   : 'output' 'bench'
     LBRACE
     (bench_out_parm_assign)+
     RBRACE
   ;  
   
 bench_out_parm_assign
   : 'add_test_command' EQ STR
   | 'generate_external_regs' EQ bool 
   | 'only_output_dut_instances' EQ bool 
   ;
   
/*   
// ------------ cppmod_out_defs
 cppmod_out_defs
   : 'output' 'cppmod'
     LBRACE
     (cppmod_out_parm_assign)+
     RBRACE
   ;  
   
 cppmod_out_parm_assign
   : 'set_no_model' EQ STR
   | 'set_lite_model' EQ STR
   | 'set_full_model' EQ STR
   ;
*/

// ------------ model_annotation
 model_annotation
   : 'annotate'
     LBRACE
     (annotation_command)+
     RBRACE
   ;

// ------------ annotation_command
 annotation_command
   : ('set_reg_property' | 'set_field_property')
     (ID | 'external') EQ STR  // external is a parms keyword, so special case ID
     ('instances' | 'components')
     STR
   ;
   
// ------------
   
bool
  : ('true' | 'false')
  ;
  
fragment LETTER : ('a'..'z'|'A'..'Z') ;

WS : [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines


SL_COMMENT
  : ( '//' ~[\r\n]* '\r'? '\n'
    ) -> skip
  ;


ML_COMMENT
    :   ( '/*' .*? '*/'
        ) -> skip
    ;


ID
  : ('\\')?
    (LETTER | '_')(LETTER | '_' | '0'..'9')*
  ;

fragment VNUM
  : '\'' ( 'b' ('0' | '1' | '_')+
         | 'd' ('0'..'9' | '_')+
         | 'o' ('0'..'7' | '_')+
         | 'h' ('0'..'9' | 'a'..'f' | 'A'..'F' | '_')+
         )
  ;

NUM
  : ('0'..'9')* (VNUM | ('0'..'9'))
  | '0x' ('0'..'9' | 'a'..'f' | 'A'..'F')+
  ;
  
fragment ESC_DQUOTE
  : '\\\"'
  ;

STR
  : '"'
      ( ~('"' | '\n' | '\\')
      | ESC_DQUOTE
      | '\n'
      )*
    '"' // "
  ;

LBRACE : '{' ;
RBRACE : '}' ;
EQ     : '=' ;
