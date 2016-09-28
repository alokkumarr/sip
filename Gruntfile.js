/**
@toc
2. load grunt plugins
3. init
4. setup variables
5. grunt.initConfig
6. register grunt tasks

*/

'use strict';

module.exports = function(grunt) {

	/**
	Load grunt plugins
	@toc 2.
	*/
	grunt.loadNpmTasks('grunt-contrib-concat');
	grunt.loadNpmTasks('grunt-contrib-less');
	grunt.loadNpmTasks('grunt-contrib-uglify');
	grunt.loadNpmTasks('grunt-contrib-cssmin');
	grunt.loadNpmTasks('grunt-contrib-jshint');
	//grunt.loadNpmTasks('grunt-karma');

	/**
	Function that wraps everything to allow dynamically setting/changing grunt options and config later by grunt task. This init function is called once immediately (for using the default grunt options, config, and setup) and then may be called again AFTER updating grunt (command line) options.
	@toc 3.
	@method init
	*/
	function init(params) {
		/**
		Project configuration.
		@toc 5.
		*/
		grunt.initConfig({
			concat: {
				devJs: {
					src:    [
						'src/module.js',
						'src/components/*.js',
						'src/highcharts.js'
					],
					dest:   'dist/lib.js'
				},
				devJsLibs: {
					src:    [
						"bower_components/angular/angular.js",
						"bower_components/angular-aria/angular-aria.js",
						"bower_components/angular-animate/angular-animate.js",
						"bower_components/angular-material/angular-material.js",
						"bower_components/jsPlumb/dist/js/jsPlumb-2.1.7.js",
						"bower_components/jquery/dist/jquery.min.js",
						"bower_components/jquery-ui/jquery-ui.min.js",
						"bower_components/highcharts/highcharts.src.js",
						"bower_components/pivottable/dist/pivot.js",
						"bower_components/angular-ui-grid/ui-grid.js"
					],
					dest:   'dist/libs.js'
				}
			},
			jshint: {
				options: {
					//force:          true,
					globalstrict:   true,
					//sub:            true,
					node: true,
					loopfunc: true,
					browser:        true,
					devel:          true,
					globals: {
						angular:    false,
						$:          false,
						moment:		false,
						Pikaday: false,
						module: false,
						forge: false
					}
				},
				beforeconcat:   {
					options: {
						force:	false,
						ignores: []
					},
					files: {
						src: []
					}
				},
				//quick version - will not fail entire grunt process if there are lint errors
				beforeconcatQ:   {
					options: {
						force:	true,
						ignores: []
					},
					files: {
						src: []
					}
				}
			},
			uglify: {
				options: {
					mangle: false
				},
				build: {
					files:  {},
					src:    ['dist/lib.js'],
					dest:   'dist/lib.min.js'
				}
			},
			less: {
				development: {
					options: {
					},
					files: {
						"css/theme.css": "themes/triton/base.less"
					}
				}
			},
			cssmin: {
				dev: {
					src: ['css/theme.css'],
					dest: 'css/theme.min.css'
				}
			}
			// karma: {
			// 	unit: {
			// 		//configFile: publicPathRelativeRoot+'config/karma.conf.js',
			// 		singleRun: true,
			// 		browsers: ['PhantomJS']
			// 	}
			// }
		});
		
		
		/**
		register/define grunt tasks
		@toc 6.
		*/
		// Default task(s).
		//grunt.registerTask('default', ['jshint:beforeconcat', 'less:development', 'concat:devJs', 'concat:devCss']);
		grunt.registerTask('default', ['jshint:beforeconcatQ', 'less:development', 'cssmin', 'concat:devJs', 'concat:devJsLibs' ,'uglify:build']);
	
	}
	init({});		//initialize here for defaults (init may be called again later within a task)

};