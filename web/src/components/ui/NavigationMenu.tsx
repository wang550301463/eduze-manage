import * as NavigationMenuPrimitive from '@radix-ui/react-navigation-menu';
import { CaretDown } from '@phosphor-icons/react';
import { forwardRef, type ComponentPropsWithoutRef } from 'react';
import { cn } from '@/lib/cn';

export const NavigationMenu = forwardRef<
  HTMLElement,
  ComponentPropsWithoutRef<typeof NavigationMenuPrimitive.Root>
>(({ className, children, ...props }, ref) => (
  <NavigationMenuPrimitive.Root
    ref={ref}
    className={cn('relative z-10 flex max-w-max flex-1 items-center justify-center', className)}
    {...props}
  >
    {children}
    <NavigationMenuViewport />
  </NavigationMenuPrimitive.Root>
));
NavigationMenu.displayName = 'NavigationMenu';

export const NavigationMenuList = forwardRef<
  HTMLUListElement,
  ComponentPropsWithoutRef<typeof NavigationMenuPrimitive.List>
>(({ className, ...props }, ref) => (
  <NavigationMenuPrimitive.List
    ref={ref}
    className={cn('group flex flex-1 list-none items-center justify-center gap-1', className)}
    {...props}
  />
));
NavigationMenuList.displayName = 'NavigationMenuList';

export const NavigationMenuItem = NavigationMenuPrimitive.Item;

export const NavigationMenuTrigger = forwardRef<
  HTMLButtonElement,
  ComponentPropsWithoutRef<typeof NavigationMenuPrimitive.Trigger>
>(({ className, children, ...props }, ref) => (
  <NavigationMenuPrimitive.Trigger
    ref={ref}
    className={cn(
      'group inline-flex h-10 items-center justify-center rounded-md bg-background px-4 py-2 text-sm font-medium hover:bg-muted',
      className,
    )}
    {...props}
  >
    {children}
    <CaretDown
      className="relative top-[1px] ml-1 h-3 w-3 transition duration-200 group-data-[state=open]:rotate-180"
      aria-hidden
    />
  </NavigationMenuPrimitive.Trigger>
));
NavigationMenuTrigger.displayName = 'NavigationMenuTrigger';

export const NavigationMenuContent = forwardRef<
  HTMLDivElement,
  ComponentPropsWithoutRef<typeof NavigationMenuPrimitive.Content>
>(({ className, ...props }, ref) => (
  <NavigationMenuPrimitive.Content
    ref={ref}
    className={cn(
      'left-0 top-0 w-full data-[motion^=from-]:animate-in data-[motion^=to-]:animate-out md:absolute md:w-auto',
      className,
    )}
    {...props}
  />
));
NavigationMenuContent.displayName = 'NavigationMenuContent';

export const NavigationMenuLink = NavigationMenuPrimitive.Link;

const NavigationMenuViewport = forwardRef<
  HTMLDivElement,
  ComponentPropsWithoutRef<typeof NavigationMenuPrimitive.Viewport>
>(({ className, ...props }, ref) => (
  <div className={cn('absolute left-0 top-full flex justify-center')}>
    <NavigationMenuPrimitive.Viewport
      className={cn(
        'origin-top-center relative mt-1.5 h-[var(--radix-navigation-menu-viewport-height)] w-full overflow-hidden rounded-md border border-border bg-background shadow-lg md:w-[var(--radix-navigation-menu-viewport-width)]',
        className,
      )}
      ref={ref}
      {...props}
    />
  </div>
));
NavigationMenuViewport.displayName = 'NavigationMenuViewport';

export const NavigationMenuIndicator = forwardRef<
  HTMLDivElement,
  ComponentPropsWithoutRef<typeof NavigationMenuPrimitive.Indicator>
>(({ className, ...props }, ref) => (
  <NavigationMenuPrimitive.Indicator
    ref={ref}
    className={cn(
      'top-full z-[1] flex h-1.5 items-end justify-center overflow-hidden data-[state=visible]:animate-in data-[state=hidden]:animate-out',
      className,
    )}
    {...props}
  >
    <div className="relative top-[60%] h-2 w-2 rotate-45 rounded-tl-sm bg-border shadow-md" />
  </NavigationMenuPrimitive.Indicator>
));
NavigationMenuIndicator.displayName = 'NavigationMenuIndicator';
